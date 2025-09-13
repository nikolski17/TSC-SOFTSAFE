package com.tsc;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record LoginRequest(String centro, String usuario, String contrasena) {}
record LoginResponse(String status, String message, Map<String, String> clienteInfo, List<Map<String, Object>> campos) {}

@RestController
public class LoginController {

    private static Map<String, Map<String, String>> clientesConfigCache;
    // IMPORTANTE: Asegúrate de que este ID sea el de tu hoja "Configuración de Clientes (TSC)"
    private static final String CONFIG_SPREADSHEET_ID = "1ARn1NbwrN4ALRNnr_qc8bI3TVLfgPEdPa8jOxiXhGBg"; 

    @CrossOrigin(origins = "http://127.0.0.1:5500")
    @GetMapping("/api/config/clientes")
    public ResponseEntity<List<String>> getClientes() {
        try { // --- PLAN B AÑADIDO ---
            if (clientesConfigCache == null) {
                cargarConfiguracionClientes();
            }
            List<String> nombresDeClientes = clientesConfigCache.values().stream()
                                     .map(map -> map.get("nombreCentro"))
                                     .sorted()
                                     .collect(Collectors.toList());
            return ResponseEntity.ok(nombresDeClientes);
        } catch (Exception e) {
            System.err.println("!!! ERROR CRÍTICO AL CARGAR LA LISTA DE CLIENTES: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
    
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    @PostMapping("/api/login")
    public ResponseEntity<LoginResponse> handleLogin(@RequestBody LoginRequest loginRequest) {
        try { // --- PLAN B AÑADIDO ---
            System.out.println(">>> Petición de login recibida para el centro: " + loginRequest.centro());

            if (clientesConfigCache == null) {
                cargarConfiguracionClientes();
            }

            Map<String, String> infoCliente = clientesConfigCache.get(loginRequest.centro().toLowerCase());
            if (infoCliente == null) {
                return ResponseEntity.ok(new LoginResponse("error", "Centro no encontrado.", null, null));
            }
            String idHojaUsuarios = infoCliente.get("idUsuarios");

            boolean esValido = verificarUsuario(idHojaUsuarios, loginRequest.centro(), loginRequest.usuario(), loginRequest.contrasena());

            if (esValido) {
                System.out.println(">>> Acceso concedido para el usuario: " + loginRequest.usuario());
                List<Map<String, Object>> camposPersonalizados = cargarCamposPersonalizados(infoCliente.get("idConfig"), loginRequest.centro());
                return ResponseEntity.ok(new LoginResponse("success", "Login exitoso", infoCliente, camposPersonalizados));
            } else {
                System.out.println(">>> Acceso denegado para el usuario: " + loginRequest.usuario());
                return ResponseEntity.ok(new LoginResponse("error", "Usuario, contraseña o centro incorrectos.", null, null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new LoginResponse("error", "Error interno del servidor: " + e.getMessage(), null, null));
        }
    }

    private void cargarConfiguracionClientes() throws IOException, GeneralSecurityException { // Informa del riesgo
        List<List<Object>> values = GoogleSheetsService.readSheetValues(CONFIG_SPREADSHEET_ID, "Clientes!A2:C");
        if(values == null || values.isEmpty()){
            clientesConfigCache = new HashMap<>();
            return;
        }
        
        clientesConfigCache = values.stream()
            .filter(row -> row.size() >= 3)
            .collect(Collectors.toMap(
                row -> row.get(0).toString().trim().toLowerCase(),
                row -> Map.of(
                    "nombreCentro", row.get(0).toString().trim(),
                    "idDatos", row.get(1).toString().trim(),
                    "idUsuarios", row.get(2).toString().trim(),
                    "idConfig", CONFIG_SPREADSHEET_ID
                )
            ));
    }

    private boolean verificarUsuario(String idHojaUsuarios, String centro, String usuario, String contrasena) throws IOException, GeneralSecurityException { // Informa del riesgo
        List<List<Object>> users = GoogleSheetsService.readSheetValues(idHojaUsuarios, "Hoja 1!A2:D");
        if (users == null || users.isEmpty()) return false;

        for (List<Object> row : users) {
            if (row.size() >= 4) {
                String centroSheet = row.get(0).toString().trim();
                String usuarioSheet = row.get(1).toString().trim();
                String contrasenaSheet = row.get(2).toString().trim();
                String estadoSheet = row.get(3).toString().trim();

                if (centroSheet.equalsIgnoreCase(centro) &&
                    usuarioSheet.equalsIgnoreCase(usuario) &&
                    contrasenaSheet.equals(contrasena) &&
                    estadoSheet.equalsIgnoreCase("Activo")) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Map<String, Object>> cargarCamposPersonalizados(String idConfig, String centro) throws IOException, GeneralSecurityException { // Informa del riesgo
        String rango = "CamposPersonalizados!A2:D";
        List<List<Object>> allFields = GoogleSheetsService.readSheetValues(idConfig, rango);
        List<Map<String, Object>> camposDelCentro = new ArrayList<>();

        if (allFields != null && !allFields.isEmpty()) {
            for (List<Object> row : allFields) {
                if (row.size() > 1 && row.get(0).toString().trim().equalsIgnoreCase(centro)) {
                    Map<String, Object> campo = new HashMap<>();
                    campo.put("nombre", row.get(1).toString());
                    campo.put("tipo", (row.size() > 2) ? row.get(2).toString() : "Texto");
                    if (row.size() >= 4 && row.get(3) != null) {
                        campo.put("opciones", row.get(3).toString().split(","));
                    } else {
                         campo.put("opciones", new String[]{});
                    }
                    camposDelCentro.add(campo);
                }
            }
        }
        return camposDelCentro;
    }
}

