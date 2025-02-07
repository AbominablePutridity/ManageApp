package com.example.manageapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_FILE = 1;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAttachFile = findViewById(R.id.btnAttachFile);
        tvResult = findViewById(R.id.tvResult);

        btnAttachFile.setOnClickListener(v -> openFilePicker());
    }

    public void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Выбор любого файла
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri fileUri = data.getData(); // Получаем URI выбранного файла
                textToJsonFormat(parseExcelFile(fileUri));
            }
        }
    }

    private String parseExcelFile(Uri fileUri) {
        String res = null;
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);

            // Определяем формат файла
            Workbook workbook;
            try {
                workbook = new XSSFWorkbook(inputStream); // Пробуем прочитать как .xlsx
            } catch (Exception e) {
                // Если не удалось, пробуем прочитать как .xls
                inputStream = getContentResolver().openInputStream(fileUri); // Переоткрываем поток
                workbook = new HSSFWorkbook(inputStream); // Читаем как .xls
            }

            Sheet sheet = workbook.getSheetAt(0); // Получаем первый лист

            StringBuilder result = new StringBuilder();
            for (Row row : sheet) {
                for (Cell cell : row) {
                    // Проверяем тип ячейки
                    switch (cell.getCellType()) {
                        case FORMULA:
                            // Если ячейка содержит формулу, получаем результат вычисления
                            switch (cell.getCachedFormulaResultType()) {
                                case NUMERIC:
                                    result.append(cell.getNumericCellValue()).append("\t");
                                    break;
                                case STRING:
                                    result.append(cell.getStringCellValue()).append("\t");
                                    break;
                                case BOOLEAN:
                                    result.append(cell.getBooleanCellValue()).append("\t");
                                    break;
                                case ERROR:
                                    result.append("ERROR").append("\t");
                                    break;
                                default:
                                    result.append(cell.toString()).append("\t");
                            }
                            break;
                        case NUMERIC:
                            result.append(cell.getNumericCellValue()).append("\t");
                            break;
                        case STRING:
                            result.append(cell.getStringCellValue()).append("\t");
                            break;
                        case BOOLEAN:
                            result.append(cell.getBooleanCellValue()).append("\t");
                            break;
                        case BLANK:
                            result.append("").append("\t");
                            break;
                        default:
                            result.append(cell.toString()).append("\t");
                    }
                }
                result.append("\n");
            }

            res = result.toString();
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
            res = "Ошибка при чтении файла: " + e.getMessage();
        }

        return res;
    }

    private void textToJsonFormat(String parsedText) {
        try {
            // Разделяем текст на строки
            String[] rows = parsedText.split("\n");
            JSONArray jsonArray = new JSONArray();

            // Предположим, что первая строка содержит заголовки столбцов
            String[] headers = rows[0].split("\t");

            // Начинаем с 1, чтобы пропустить заголовки
            for (int i = 1; i < rows.length; i++) {
                String[] columns = rows[i].split("\t");
                JSONObject jsonObject = new JSONObject();

                for (int j = 0; j < headers.length; j++) {
                    // Добавляем каждый столбец как ключ-значение в JSON объект
                    jsonObject.put(headers[j], columns[j]);
                }

                // Добавляем JSON объект в массив
                jsonArray.put(jsonObject);
            }

            // Выводим результат в TextView (или используем его как нужно)
            tvResult.setText(jsonArray.toString(2)); // 2 - это отступ для красивого форматирования
        } catch (Exception e) {
            e.printStackTrace();
            tvResult.setText("Ошибка при преобразовании в JSON: " + e.getMessage());
        }
    }
}