// IGNORE_K2
public class J {
    private String s1;
    private String s2;
    private String s3;

    public J(String s1) {
        this.s1 = s1;
    }
    public J(String s1, String s2) {
        this(s1);
        this.s2 = s2;
    }
    public J(String s1, String s2, String s3) {
        this(s1, s2);
        this.s3 = s3;
    }
}