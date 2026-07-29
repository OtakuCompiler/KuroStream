public class TestLoad {
    public static void main(String[] args) {
        try {
            String path = System.getProperty("org.sqlite.lib.path");
            String name = System.getProperty("org.sqlite.lib.name");
            System.out.println("path=" + path);
            System.out.println("name=" + name);
            if (path != null && name != null) {
                java.io.File f = new java.io.File(path, name);
                System.out.println("File exists: " + f.exists());
                System.out.println("Absolute path: " + f.getAbsolutePath());
                System.load(f.getAbsolutePath());
                System.out.println("LOADED OK");
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }
}
