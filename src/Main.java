import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Calculadora web: backend em Java puro (sem bibliotecas externas).
 *
 *  - GET /api/calcular?expr=2%2B3*4  ->  {"resultado":"14"}
 *  - Qualquer outro caminho serve os arquivos da pasta "public" (o frontend).
 */
public class Main {

    public static void main(String[] args) throws IOException {
        // Plataformas de hospedagem informam a porta pela variável PORT.
        int porta = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Path pastaPublica = Paths.get("public").toAbsolutePath().normalize();

        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext("/api/calcular", Main::calcular);
        servidor.createContext("/", troca -> arquivoEstatico(troca, pastaPublica));
        servidor.start();

        System.out.println("Calculadora no ar: http://localhost:" + porta);
    }

    // ---------------------------------------------------------------- API

    private static void calcular(HttpExchange troca) throws IOException {
        if (!troca.getRequestMethod().equals("GET")) {
            enviarJson(troca, 405, "{\"erro\":\"Método não permitido\"}");
            return;
        }

        String expressao = lerParametro(troca.getRequestURI().getRawQuery(), "expr");
        if (expressao == null || expressao.isBlank()) {
            enviarJson(troca, 400, "{\"erro\":\"Digite uma expressão\"}");
            return;
        }
        if (expressao.length() > 200) {
            enviarJson(troca, 400, "{\"erro\":\"Expressão muito longa\"}");
            return;
        }

        try {
            double valor = new Avaliador(expressao).avaliar();
            enviarJson(troca, 200, "{\"resultado\":\"" + formatar(valor) + "\"}");
        } catch (IllegalArgumentException | ArithmeticException e) {
            enviarJson(troca, 400, "{\"erro\":\"" + escaparJson(e.getMessage()) + "\"}");
        }
    }

    private static String lerParametro(String consulta, String nome) {
        if (consulta == null) return null;
        for (String par : consulta.split("&")) {
            int i = par.indexOf('=');
            if (i > 0 && par.substring(0, i).equals(nome)) {
                return URLDecoder.decode(par.substring(i + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /** Arredonda para 12 dígitos significativos e remove zeros à direita (0.1+0.2 -> 0.3). */
    private static String formatar(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new ArithmeticException("Resultado inválido");
        }
        BigDecimal numero = new BigDecimal(valor).round(new MathContext(12)).stripTrailingZeros();
        if (numero.signum() == 0) return "0";
        double abs = Math.abs(valor);
        return (abs >= 1e15 || abs < 1e-9) ? numero.toString() : numero.toPlainString();
    }

    private static String escaparJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ------------------------------------------------------- Avaliador

    /** Calcula expressões com + - * / % e parênteses (descida recursiva). */
    static class Avaliador {
        private final String s;
        private int pos = 0;

        Avaliador(String texto) {
            this.s = texto.replace(" ", "").replace(',', '.').replace('×', '*').replace('÷', '/');
        }

        double avaliar() {
            double valor = expressao();
            if (pos < s.length()) {
                throw new IllegalArgumentException("Caractere inesperado: '" + s.charAt(pos) + "'");
            }
            return valor;
        }

        // expressao := termo (('+' | '-') termo)*
        private double expressao() {
            double valor = termo();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '+') { pos++; valor += termo(); }
                else if (c == '-') { pos++; valor -= termo(); }
                else break;
            }
            return valor;
        }

        // termo := fator (('*' | '/') fator)*
        private double termo() {
            double valor = fator();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '*') { pos++; valor *= fator(); }
                else if (c == '/') {
                    pos++;
                    double divisor = fator();
                    if (divisor == 0) throw new ArithmeticException("Divisão por zero");
                    valor /= divisor;
                } else break;
            }
            return valor;
        }

        // fator := ('-' | '+') fator | (numero | '(' expressao ')') '%'*
        private double fator() {
            if (pos >= s.length()) throw new IllegalArgumentException("Expressão incompleta");
            char c = s.charAt(pos);

            if (c == '-') { pos++; return -fator(); }
            if (c == '+') { pos++; return fator(); }

            double valor;
            if (c == '(') {
                pos++;
                valor = expressao();
                if (pos >= s.length() || s.charAt(pos) != ')') {
                    throw new IllegalArgumentException("Parêntese não fechado");
                }
                pos++;
            } else {
                int inicio = pos;
                while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
                if (inicio == pos) throw new IllegalArgumentException("Caractere inesperado: '" + c + "'");
                try {
                    valor = Double.parseDouble(s.substring(inicio, pos));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Número inválido");
                }
            }

            while (pos < s.length() && s.charAt(pos) == '%') { pos++; valor /= 100; }
            return valor;
        }
    }

    // ------------------------------------------------ Arquivos estáticos

    private static void arquivoEstatico(HttpExchange troca, Path pasta) throws IOException {
        String caminho = troca.getRequestURI().getPath();
        if (caminho.equals("/")) caminho = "/index.html";

        Path arquivo;
        try {
            arquivo = pasta.resolve(caminho.substring(1)).normalize();
        } catch (InvalidPathException e) {
            enviar(troca, 404, "text/plain; charset=utf-8", "Não encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }

        // Impede acessar arquivos fora da pasta "public" (ex.: /../../etc/passwd).
        if (!arquivo.startsWith(pasta) || !Files.isRegularFile(arquivo)) {
            enviar(troca, 404, "text/plain; charset=utf-8", "Não encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        enviar(troca, 200, tipoDoArquivo(arquivo.getFileName().toString()), Files.readAllBytes(arquivo));
    }

    private static String tipoDoArquivo(String nome) {
        if (nome.endsWith(".html")) return "text/html; charset=utf-8";
        if (nome.endsWith(".css")) return "text/css; charset=utf-8";
        if (nome.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (nome.endsWith(".svg")) return "image/svg+xml";
        if (nome.endsWith(".png")) return "image/png";
        if (nome.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    // ---------------------------------------------------------- Resposta

    private static void enviarJson(HttpExchange troca, int status, String json) throws IOException {
        enviar(troca, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void enviar(HttpExchange troca, int status, String tipo, byte[] corpo) throws IOException {
        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.sendResponseHeaders(status, corpo.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(corpo);
        }
    }
}
