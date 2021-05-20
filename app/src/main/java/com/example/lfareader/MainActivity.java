package com.example.lfareader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    List<String> testNameArray = new ArrayList<>();

    private String testName;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testNameArray.add("Select Test");
        testNameArray.add("Toponin T");
        testNameArray.add("Troponin I");

        Spinner spin = (Spinner) findViewById(R.id.testNameSpinner);
        ArrayAdapter array = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, testNameArray);
        array.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spin.setAdapter(array);
        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                testName = spin.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        FloatingActionButton addNewSample = (FloatingActionButton) findViewById(R.id.addNewSampleBtn);
        addNewSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCameraActivity();
            }
        });


    }

    //function called when new test is added in settings
    private void addTestNamesToList(String newTestName){
        testNameArray.add(newTestName);
    }

    private void createFileName (){
        EditText patientNameText = (EditText) findViewById(R.id.patientNameFld);
        String patientName = patientNameText.getText().toString();
        EditText patientIDText = (EditText) findViewById(R.id.patientIDFld);
        String patientID = patientIDText.getText().toString();
        EditText sampleIDText = (EditText) findViewById(R.id.sampleIDFld);
        String sampleID = sampleIDText.getText().toString();
        fileName = testName + "_" + patientName + "_" + patientID + "_" + sampleID;
    }

    private void launchCameraActivity(){
        createFileName();
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("fileName", fileName);
        startActivity(intent);
    }

    public void startImageProcessing (String imagePath) {
        Log.i("startImageProcessingTag", "From main activity"+imagePath);
    }
    
}