public interface ClientFactory {
    <T> T newClient(Class<T> client);
}
