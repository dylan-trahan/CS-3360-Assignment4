public class Task {
    public int period;
    public int execution_time;
    public int deadline;
    public int remaining_time;

    public Task(int period, int execution_time){
        this.period = period;
        this.execution_time = execution_time;
        this.remaining_time = execution_time;
    }
}
