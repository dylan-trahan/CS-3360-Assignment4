public class Process {
    public int id;
    public double arrival_time;
    public double service_time;
    public double remaining_service_time;
    public double start_time;
    public double completion_time;
    public int priority;

    @Override
    public String toString(){
        return "[id=" + id + ", service=" + service_time + ", arrival=" + arrival_time + ", remaining=" + remaining_service_time + ", start=" + start_time +"]";
    }

    public boolean equals(Process p){
        return p.id == this.id;
    }

}
