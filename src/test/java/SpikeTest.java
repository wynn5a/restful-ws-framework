import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author wynn5a
 */
public class SpikeTest {

  Server server;

  @BeforeEach
  public void setup() throws Exception {
    server = new Server(8888);
    Connector connector = new ServerConnector(server);
    server.addConnector(connector);
    ServletContextHandler handler = new ServletContextHandler(server, "/");
    handler.addServlet(new ServletHolder(new TestServlet(new TestApplication())), "/");
    server.setHandler(handler);
    server.start();
  }

  @AfterEach
  public void teardown() throws Exception {
    server.stop();
  }

  @Test
  public void should_start_server() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(new URI("http://127.0.0.1:8888/")).GET().build();
    HttpResponse<String> resp = client.send(request, BodyHandlers.ofString());
    Assertions.assertEquals("hello", resp.body());
  }

}

class TestApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    return Set.of(TestResource.class);
  }
}

class TestServlet extends HttpServlet {

  Application application;

  public TestServlet(Application application) {
    this.application = application;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Stream<Class<?>> rootClasses = application.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class));
    Object result = dispatch(req, rootClasses);
    resp.getWriter().write(result.toString());
    resp.getWriter().flush();
  }

  private Object dispatch(HttpServletRequest req, Stream<Class<?>> rootClasses) {
    Class<?> root = rootClasses.findFirst().get();
    Method method = Arrays.stream(root.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst()
                          .get();
    try {
      Object o = root.getDeclaredConstructor().newInstance();
      return method.invoke(o);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

@Path("/hello")
class TestResource {

  @GET
  public String get() {
    return "hello";
  }
}
