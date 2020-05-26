package com.cactus.mycactusdisease;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

public class ResultActivity extends AppCompatActivity {

    private ImageView imageResult, resultBtnBack;
    private TextView diseaseName, textResult, resultBottomBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        initialVariables();
        setAction();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            int index = bundle.getInt("Max");
            byte[] image = bundle.getByteArray("Image");
            predictionResult(String.valueOf(index));

            if (image != null) {
                imageResult.setImageDrawable(new BitmapDrawable(this.getResources(),
                        BitmapFactory.decodeByteArray(image, 0, image.length)));
            }
        }
    }

    public void predictionResult(String index) {
        switch (index) {
            case "0":
                diseaseName.setText("เพลี้ย");
                textResult.setText("วิธีการรักษา\n" +
                        "กำจัดโดยการเก็บทิ้งโดยใช้แอลกอฮอล์เช็ดที่ผิวต้นหากพบในปริมาณมากให้ฉีดพ่นด้วยสารประเภทดูดซึมอย่างมาลาไธออนหรือนิโคติลซัลเฟต");
                break;
            case "1":
                diseaseName.setText("โรคราสนิม");
                textResult.setText("วิธีการรักษา\n" +
                        "การรักษาตำหนิให้หายต้องใช้วิธีการรักษาโดยรอให้ต้นไม้โตไล่ตำหนิลง ซึ่งมีระยะเวลานาน");
                break;
            case "2":
                diseaseName.setText("โรคเชื้อรา");
                textResult.setText("วิธีการรักษา\n" +
                        "ตัดส่วนที่เน่าทิ้งไป โดยตัดให้เหนือแผลประมาณ 1-2 นิ้วใช้คอปเปอร์ซัลเฟตหรือยาฆ่าเชื้อราทารอยตัดและบริเวณใกล้เคียงให้ทั่ว");
                break;
        }

    }

    private void initialVariables() {
        imageResult = (ImageView) findViewById(R.id.imageResult);
        diseaseName = (TextView) findViewById(R.id.diseaseName);
        resultBtnBack = (ImageView) findViewById(R.id.resultBtnBack);
        textResult = (TextView) findViewById(R.id.textResult);
    }

    private void setAction() {
        resultBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
