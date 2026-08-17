package br.com.fiap.adapters.in.web;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padrão de erro da API")
public class ApiErrorResponse {

    @Schema(
        description = "Data e hora em que o erro ocorreu",
        example = "2026-08-12T10:30:00",
        format = "date-time"
    )
    private LocalDateTime timestamp;

    @Schema(
        description = "Código HTTP do erro",
        example = "400"
    )
    private int status;

    @Schema(
        description = "Descrição do erro HTTP",
        example = "Bad Request"
    )
    private String error;

    @Schema(
        description = "Mensagem descritiva do erro",
        example = "O nome é obrigatório"
    )
    private String message;

    @Schema(
        description = "Caminho da requisição que gerou o erro",
        example = "/api/v1/clientes"
    )
    private String path;

    public ApiErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
}
