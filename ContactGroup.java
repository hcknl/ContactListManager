public class ContactGroup {

    public int id;
    public String title;
    public String accountName;
    public String accountType;

    @Override
    public String toString() {
        return "ContactGroup{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", accountName='" + accountName + '\'' +
                ", accountType='" + accountType + '\'' +
                '}';
    }
}
