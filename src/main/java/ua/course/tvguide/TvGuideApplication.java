package ua.course.tvguide;

import com.sun.net.httpserver.HttpServer;
import ua.course.tvguide.repository.TvGuideRepository;
import ua.course.tvguide.web.ApiHandler;
import ua.course.tvguide.web.StaticFileHandler;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class TvGuideApplication {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path projectRoot = Path.of("").toAbsolutePath();

        TvGuideRepository repository = new TvGuideRepository(projectRoot.resolve("data"));
        repository.load();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new ApiHandler(repository));
        server.createContext("/", new StaticFileHandler(projectRoot.resolve("src/main/resources/public")));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("Телепрограма запущена: http://localhost:" + port);
        System.out.println("Адміністратор: admin / admin123");
    }
}
