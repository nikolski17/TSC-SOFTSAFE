package com.tsc;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "TSC SOFTSAFE Backend";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.home") + "/tokens_tsc_softsafe";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static Sheets serviceInstance;

    private static Credential getCredentials() throws IOException, GeneralSecurityException {
        InputStream in = GoogleSheetsService.class.getResourceAsStream("/credentials.json");
        if (in == null) {
            throw new FileNotFoundException("Recurso no encontrado: credentials.json. Asegúrate de que esté en src/main/resources.");
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static Sheets getSheetsService() throws IOException, GeneralSecurityException {
        if (serviceInstance == null) {
            Credential credential = getCredentials();
            serviceInstance = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
        return serviceInstance;
    }

    public static void appendToSheet(String spreadsheetId, String range, List<Object> rowData) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        List<List<Object>> values = Collections.singletonList(rowData);
        ValueRange body = new ValueRange().setValues(values);

        AppendValuesResponse result = service.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
        // Mensaje de éxito en la consola del servidor
        System.out.println(">>> Registro guardado exitosamente. Celdas actualizadas: " + result.getUpdates().getUpdatedCells());
    }
    
    public static List<List<Object>> readSheetValues(String spreadsheetId, String range) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
        return response.getValues();
    }

    public static void updateCellValue(String spreadsheetId, String range, String value) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        List<Object> rowData = new ArrayList<>();
        rowData.add(value);
        List<List<Object>> values = new ArrayList<>();
        values.add(rowData);

        ValueRange body = new ValueRange().setValues(values);
        
        UpdateValuesResponse result = service.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
        // Mensaje de éxito en la consola del servidor
        System.out.println(">>> Celda actualizada exitosamente en el rango: " + result.getUpdatedRange());
    }
}

