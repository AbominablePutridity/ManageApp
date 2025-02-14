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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

//    public String[] STATIC_COLUMN_NAME = new String[] {
//        "typeOfWeek",
//        "number",
//        "group"
//    };

//    StringBuilder[][] resultArray = new StringBuilder[5000][5000];
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
                outArray(parseExcelFile(fileUri));

                try {
                    tvResult.setText(convertDataToJson(parseExcelFile(fileUri)));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String[][] parseExcelFile(Uri fileUri) {
        String[][] resultArray = null;
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

            // Определяем количество строк и столбцов
            int rowCount = sheet.getPhysicalNumberOfRows();
            int colCount = 0;
            for (Row row : sheet) {
                if (row.getPhysicalNumberOfCells() > colCount) {
                    colCount = row.getPhysicalNumberOfCells();
                }
            }

            // Создаем двумерный массив для хранения данных
            resultArray = new String[rowCount][colCount];

            // Заполняем массив данными из ячеек, пропуская пустые ячейки
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    for (int cellIndex = 0; cellIndex < colCount; cellIndex++) {
                        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        if (cell.getCellType() != CellType.BLANK) { // Пропускаем пустые ячейки
                            switch (cell.getCellType()) {
                                case FORMULA:
                                    // Если ячейка содержит формулу, получаем результат вычисления
                                    switch (cell.getCachedFormulaResultType()) {
                                        case NUMERIC:
                                            resultArray[rowIndex][cellIndex] = String.valueOf(cell.getNumericCellValue());
                                            break;
                                        case STRING:
                                            resultArray[rowIndex][cellIndex] = cell.getStringCellValue();
                                            break;
                                        case BOOLEAN:
                                            resultArray[rowIndex][cellIndex] = String.valueOf(cell.getBooleanCellValue());
                                            break;
                                        case ERROR:
                                            resultArray[rowIndex][cellIndex] = "ERROR";
                                            break;
                                        default:
                                            resultArray[rowIndex][cellIndex] = cell.toString();
                                    }
                                    break;
                                case NUMERIC:
                                    resultArray[rowIndex][cellIndex] = String.valueOf(cell.getNumericCellValue());
                                    break;
                                case STRING:
                                    resultArray[rowIndex][cellIndex] = cell.getStringCellValue();
                                    break;
                                case BOOLEAN:
                                    resultArray[rowIndex][cellIndex] = String.valueOf(cell.getBooleanCellValue());
                                    break;
                                default:
                                    resultArray[rowIndex][cellIndex] = cell.toString();
                            }
                        } else {
                            resultArray[rowIndex][cellIndex] = null; // Пустые ячейки будут null
                        }
                    }
                }
            }

            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
            // В случае ошибки возвращаем null
            return null;
        }

        return resultArray;
    }

    private void outArray(String[][] parsedArray) {
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 100; j++) {
                System.out.println("arr[" + i + "][" + j + "]=" +parsedArray[i][j]);
            }
        }
    }

    private String convertDataToJson(String[][] dataArray) throws JSONException {
        JSONArray jsonArray = new JSONArray();

        // Основной цикл для формирования JSON
        for (int i = 0; i < dataArray.length; i++) {
            for (int j = 0; j < dataArray[i].length; j++) {
                if ((i == 8) && ((j >= 3) && (j % 2 != 0) && (dataArray[i][j] != null))) { // формирую группы
                    JSONObject jsonObjectGroup = new JSONObject(); // Создаем JSON-объект для каждой группы
                    jsonObjectGroup.put("group", dataArray[i][j]); // Добавляем группу

                    // Создаем массив для объектов "type"
                    JSONArray typeOfDayJsonArray = new JSONArray();


                    int dayCaller = 9;
                    int round = 1;
                    // Заполняем typeOfDayJsonArray
                    for (int k = 9; k < (15 * 6); k += 15) {
                        JSONObject typeObject = new JSONObject(); // Создаем объект для каждого "type"
                        typeObject.put("type", dataArray[k][1]); // Добавляем значение с ключом "type"

                        // Создаем массив для "lesson"
                        JSONArray lessonJsonArray = new JSONArray();

                        dayCaller++;
                        for (int a = dayCaller; a < 10 + ((2 * 7) * round); a += 2) {
                                JSONObject lessonObject = new JSONObject();
                                lessonObject.put("lesson", dataArray[a][3]); // Добавляем урок
                                lessonJsonArray.put(lessonObject);
                        }
                        dayCaller += 2*7;
                        round++;

                        // Добавляем массив "lesson" в объект "type"
                        typeObject.put("lessons", lessonJsonArray);

                        // Добавляем объект "type" в массив
                        typeOfDayJsonArray.put(typeObject);
                    }

                    // Добавляем массив "type" в объект группы
                    jsonObjectGroup.put("day", typeOfDayJsonArray);

                    // Добавляем объект группы в общий массив
                    jsonArray.put(jsonObjectGroup);
                }
            }
        }

        return jsonArray.toString(4);
    }
}