public class Event implements Cloneable {
    public EventType type;
    public double time;
    public Process process;

    private Event(){}

    public Event(EventType type, double time, Process process){
        this.type = type;
        this.time = time;
        this.process = process;
    }

    @Override
    public String toString(){
        return "[type=" + type + ", time=" + time + ", process=" + ((process != null) ? process.toString() : "null") + "]"; 
    }

    public Event copy() {
        Event out = new Event();
        out.type = this.type;
        out.time = this.time;
        Process p = new Process();
        p.arrival_time = this.process.arrival_time;
        p.completion_time = this.process.completion_time;
        p.id = this.process.id;
        p.remaining_service_time = this.process.remaining_service_time;
        p.service_time = this.process.service_time;
        p.start_time = this.process.start_time;
        p.priority = this.process.priority;
        out.process = p;
        return out;
    }
}
