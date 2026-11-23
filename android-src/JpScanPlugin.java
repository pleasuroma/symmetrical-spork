package com.jparklab.baroscan;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Base64;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** 구글 ML Kit: 문서 스캐너(자동 테두리·원근 보정) + 한글/영문 글자 인식 */
@CapacitorPlugin(name = "JpScan")
public class JpScanPlugin extends Plugin {
    private ActivityResultLauncher<IntentSenderRequest> launcher;
    private PluginCall pending;
    private TextRecognizer recognizer;

    @Override
    public void load() {
        launcher = getActivity().registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), this::onScanResult);
    }

    @PluginMethod
    public void scan(PluginCall call) {
        int limit = call.getInt("pageLimit", 1);
        GmsDocumentScannerOptions opts = new GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(Math.max(1, Math.min(limit, 100)))
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build();
        GmsDocumentScanning.getClient(opts).getStartScanIntent(getActivity())
            .addOnSuccessListener(sender -> {
                pending = call;
                launcher.launch(new IntentSenderRequest.Builder(sender).build());
            })
            .addOnFailureListener(e -> call.reject("스캐너를 열 수 없습니다: " + e.getMessage()));
    }

    private void onScanResult(ActivityResult r) {
        PluginCall call = pending;
        pending = null;
        if (call == null) return;
        JSArray pages = new JSArray();
        if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null) {
            GmsDocumentScanningResult res = GmsDocumentScanningResult.fromActivityResultIntent(r.getData());
            if (res != null && res.getPages() != null) {
                int i = 0;
                for (GmsDocumentScanningResult.Page p : res.getPages()) {
                    try {
                        File out = new File(getContext().getCacheDir(), "scan_" + System.currentTimeMillis() + "_" + (i++) + ".jpg");
                        try (InputStream in = getContext().getContentResolver().openInputStream(p.getImageUri()); OutputStream os = new FileOutputStream(out)) {
                            byte[] buf = new byte[65536];
                            int n;
                            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                        }
                        pages.put(Uri.fromFile(out).toString());
                    } catch (Exception e) {
                        // 한 장이 실패해도 나머지는 넘긴다
                    }
                }
            }
        }
        JSObject ret = new JSObject();
        ret.put("pages", pages);
        call.resolve(ret);
    }

    @PluginMethod
    public void recognize(PluginCall call) {
        String b64 = call.getString("base64");
        if (b64 == null) { call.reject("이미지가 없습니다"); return; }
        byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bmp == null) { call.reject("이미지를 읽을 수 없습니다"); return; }
        if (recognizer == null) recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        recognizer.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener(text -> {
                JSArray lines = new JSArray();
                for (Text.TextBlock b : text.getTextBlocks()) {
                    for (Text.Line l : b.getLines()) {
                        Rect bb = l.getBoundingBox();
                        JSObject o = new JSObject();
                        o.put("text", l.getText());
                        o.put("h", bb == null ? 0 : bb.height());
                        o.put("x", bb == null ? 0 : bb.left);
                        o.put("y", bb == null ? 0 : bb.top);
                        lines.put(o);
                    }
                }
                JSObject ret = new JSObject();
                ret.put("lines", lines);
                call.resolve(ret);
            })
            .addOnFailureListener(e -> call.reject("글자 인식 실패: " + e.getMessage()));
    }
}
