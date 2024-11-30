import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RPCTest {
    private static volatile ServerFactoryImpl serverFactory;
    private static Thread serverThread;
    private static final int port = 8080;


    @BeforeAll
    public static void setup() {
        serverThread = new Thread(() -> {
            serverFactory = new ServerFactoryImpl();
            serverFactory.listen(port, new SomeServiceImpl());
        });

        serverThread.start();
    }

    @AfterAll
    public static void teardown() {
        serverFactory.close();
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testServerMultipleArguments() {
        var clientFactory = new ClientFactoryImpl("localhost", port);
        var client = clientFactory.newClient(SomeService.class);

        assertEquals(client.concatenate("Hello, ", "World!"), "Hello, World!");
    }

    @Test
    public void testServerSingleArgument() {
        var clientFactory = new ClientFactoryImpl("localhost", port);
        var client = clientFactory.newClient(SomeService.class);

        assertEquals(client.countSize("Hello"), "Hello".length());
    }

    @Test
    public void testServerNoArguments() {
        var clientFactory = new ClientFactoryImpl("localhost", port);
        var client = clientFactory.newClient(SomeService.class);

        assertEquals(client.constMethod(), 1);
    }

    @Test
    public void testServerExceptionPropagation() {
        var clientFactory = new ClientFactoryImpl("localhost", port);
        var client = clientFactory.newClient(SomeService.class);

        assertThrows(RuntimeException.class, () -> client.throwsException(true));
        assertDoesNotThrow(() -> client.throwsException(false));
    }

    private interface SomeService {
        Number countSize(String s);
        Number constMethod();
        String concatenate(String s1, String s2);
        Number throwsException(Boolean doThrow);
    }

    private static class SomeServiceImpl implements SomeService {
        @Override
        public Number countSize(String s) {
            return s.length();
        }

        @Override
        public String concatenate(String s1, String s2) {
            return s1 + s2;
        }

        @Override
        public Number constMethod() {
            return 1;
        }

        @Override
        public Number throwsException(Boolean doThrow) {
            if (doThrow) {
                throw new RuntimeException("remote exception");
            } else {
                return 1;
            }
        }
    }
}