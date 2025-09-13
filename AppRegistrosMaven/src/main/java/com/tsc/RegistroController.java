package com.tsc;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

record RegistroRequest(String idHojaDatos, List<String> ordenDeCampos, Map<String, String> datosFormulario) {}
record RegistroResponse(String status, String message) {}

@RestController
public class RegistroController {

    @CrossOrigin(origins = "http://127.0.0.1:5500")
    @PostMapping("/api/registrar")
    public ResponseEntity<RegistroResponse> handleRegistro(@RequestBody RegistroRequest registroRequest) {
        try { // --- PLAN B AÑADIDO ---
            List<Object> filaParaGuardar = reordenarYPrepararDatos(registroRequest.ordenDeCampos(), registroRequest.datosFormulario());
            
            GoogleSheetsService.appendToSheet(registroRequest.idHojaDatos(), "Hoja 1", filaParaGuardar);
            
            return ResponseEntity.ok(new RegistroResponse("success", "Registro guardado exitosamente."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new RegistroResponse("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

    private List<Object> reordenarYPrepararDatos(List<String> ordenDeCampos, Map<String, String> datosFormulario) {
        List<Object> filaOrdenada = new ArrayList<>();
        
        // El primer dato es siempre la fecha actual
        filaOrdenada.add(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

        // Los siguientes datos vienen del formulario, en el orden que nos dice la configuración
        for (String nombreCampo : ordenDeCampos) {
            // Usamos getOrDefault para evitar errores si un campo no viene en el mapa
            filaOrdenada.add(datosFormulario.getOrDefault(nombreCampo, "")); 
        }
        
        return filaOrdenada;
    }
}

        