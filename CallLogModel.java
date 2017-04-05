public class CallLogModel {

    private int duration;
    private String phone;


    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    // needed for using Comparator, see Java source about comparator usage
    @Override
    public int hashCode() {
        return String.valueOf(phone).hashCode();
    }
    // needed for using Comparator, see Java source about comparator usage
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CallLogModel))
            return false;
        CallLogModel c = (CallLogModel) obj;
        return String.valueOf(c.getPhone()).equals(String.valueOf(phone));
    }

    @Override
    public String toString() {
        return "CallLogModel{" +
                "duration=" + duration +
                ", phone='" + phone + '\'' +
                '}';
    }
}
