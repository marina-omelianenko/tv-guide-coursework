package ua.course.tvguide.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ua.course.tvguide.model.Channel;
import ua.course.tvguide.model.Program;
import ua.course.tvguide.repository.TvGuideRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public class ApiHandler implements HttpHandler {
    private static final String ADMIN_TOKEN = "course-admin-token";
    private final TvGuideRepository repository;

    public ApiHandler(TvGuideRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            addDefaultHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                send(exchange, 204, "");
                return;
            }
            route(exchange);
        } catch (SecurityException exception) {
            send(exchange, 401, JsonUtil.message(exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            send(exchange, 400, JsonUtil.message(exception.getMessage()));
        } catch (NoSuchElementException exception) {
            send(exchange, 404, JsonUtil.message(exception.getMessage()));
        } catch (Exception exception) {
            send(exchange, 500, JsonUtil.message("Помилка сервера: " + exception.getMessage()));
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String apiPath = path.substring("/api".length());

        if (apiPath.equals("/health") && method.equals("GET")) {
            send(exchange, 200, JsonUtil.message("Телепрограма працює"));
            return;
        }

        if (apiPath.equals("/login") && method.equals("POST")) {
            login(exchange);
            return;
        }

        if (apiPath.equals("/channels")) {
            if (method.equals("GET")) {
                send(exchange, 200, channelsJson(repository.listChannels()));
                return;
            }
            if (method.equals("POST")) {
                requireAdmin(exchange);
                Channel channel = channelFromBody(exchange);
                send(exchange, 201, channelJson(repository.createChannel(channel)));
                return;
            }
        }

        if (apiPath.startsWith("/channels/")) {
            int id = extractId(apiPath, "/channels/");
            if (method.equals("PUT")) {
                requireAdmin(exchange);
                send(exchange, 200, channelJson(repository.updateChannel(id, channelFromBody(exchange))));
                return;
            }
            if (method.equals("DELETE")) {
                requireAdmin(exchange);
                repository.deleteChannel(id);
                send(exchange, 200, JsonUtil.message("Канал видалено"));
                return;
            }
        }

        if (apiPath.equals("/programs")) {
            if (method.equals("GET")) {
                Map<String, String> query = queryParams(exchange);
                Optional<Integer> channelId = Optional.ofNullable(query.get("channelId"))
                        .filter(value -> !value.isBlank())
                        .map(Integer::parseInt);
                String sort = query.getOrDefault("sort", "time");
                send(exchange, 200, programsJson(repository.listPrograms(channelId, sort)));
                return;
            }
            if (method.equals("POST")) {
                requireAdmin(exchange);
                Program program = programFromBody(exchange);
                send(exchange, 201, programJson(repository.createProgram(program)));
                return;
            }
        }

        if (apiPath.startsWith("/programs/")) {
            int id = extractId(apiPath, "/programs/");
            if (method.equals("PUT")) {
                requireAdmin(exchange);
                send(exchange, 200, programJson(repository.updateProgram(id, programFromBody(exchange))));
                return;
            }
            if (method.equals("DELETE")) {
                requireAdmin(exchange);
                repository.deleteProgram(id);
                send(exchange, 200, JsonUtil.message("Передачу видалено"));
                return;
            }
        }

        send(exchange, 404, JsonUtil.message("Маршрут не знайдено"));
    }

    private void login(HttpExchange exchange) throws IOException {
        Map<String, String> body = JsonUtil.parseObject(readBody(exchange));
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");

        if ("admin".equals(username) && "admin123".equals(password)) {
            send(exchange, 200, "{" + JsonUtil.property("token", ADMIN_TOKEN) + "}");
            return;
        }
        throw new SecurityException("Неправильний логін або пароль");
    }

    private Channel channelFromBody(HttpExchange exchange) throws IOException {
        Map<String, String> body = JsonUtil.parseObject(readBody(exchange));
        return new Channel(
                0,
                body.getOrDefault("name", "").trim(),
                body.getOrDefault("category", "").trim()
        );
    }

    private Program programFromBody(HttpExchange exchange) throws IOException {
        Map<String, String> body = JsonUtil.parseObject(readBody(exchange));
        return new Program(
                0,
                parseInt(body.get("channelId"), "channelId"),
                body.getOrDefault("title", "").trim(),
                body.getOrDefault("description", "").trim(),
                body.getOrDefault("date", "").trim(),
                body.getOrDefault("startTime", "").trim(),
                body.getOrDefault("endTime", "").trim(),
                body.getOrDefault("genre", "").trim(),
                parseInt(body.getOrDefault("ageRating", "0"), "ageRating")
        );
    }

    private String channelsJson(List<Channel> channels) {
        return "[" + channels.stream().map(this::channelJson).reduce((left, right) -> left + "," + right).orElse("") + "]";
    }

    private String programsJson(List<Program> programs) {
        return "[" + programs.stream().map(this::programJson).reduce((left, right) -> left + "," + right).orElse("") + "]";
    }

    private String channelJson(Channel channel) {
        return "{" +
                JsonUtil.property("id", channel.getId()) + "," +
                JsonUtil.property("name", channel.getName()) + "," +
                JsonUtil.property("category", channel.getCategory()) +
                "}";
    }

    private String programJson(Program program) {
        return "{" +
                JsonUtil.property("id", program.getId()) + "," +
                JsonUtil.property("channelId", program.getChannelId()) + "," +
                JsonUtil.property("channelName", repository.channelName(program.getChannelId())) + "," +
                JsonUtil.property("title", program.getTitle()) + "," +
                JsonUtil.property("description", program.getDescription()) + "," +
                JsonUtil.property("date", program.getDate()) + "," +
                JsonUtil.property("startTime", program.getStartTime()) + "," +
                JsonUtil.property("endTime", program.getEndTime()) + "," +
                JsonUtil.property("genre", program.getGenre()) + "," +
                JsonUtil.property("ageRating", program.getAgeRating()) +
                "}";
    }

    private int extractId(String path, String prefix) {
        try {
            return Integer.parseInt(path.substring(prefix.length()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Некоректний ідентифікатор");
        }
    }

    private int parseInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Поле " + fieldName + " має бути числом");
        }
    }

    private void requireAdmin(HttpExchange exchange) {
        String token = exchange.getRequestHeaders().getFirst("X-Admin-Token");
        if (!ADMIN_TOKEN.equals(token)) {
            throw new SecurityException("Потрібен вхід адміністратора");
        }
    }

    private Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }

        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void addDefaultHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Admin-Token");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
