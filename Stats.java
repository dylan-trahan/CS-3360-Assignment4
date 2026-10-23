public class Stats {
    public double completion_time;
    public double avg_turnaround_time;
    public double avg_waiting_time;
    public double avg_response_time;
    public double utilization;
    public double throughput;
    public double avg_ready_queue_length;

    public void print(){
        System.out.printf("Completion Time: %.2fms\n", completion_time);
        System.out.printf("Average Turnaround Time: %.2fms\n", avg_turnaround_time);
        System.out.printf("Average Waiting Time: %.2fms\n", avg_waiting_time);
        System.out.printf("Average Response Time: %.2fms\n", avg_response_time);
        System.out.printf("Utilization: %.2f%%\n",utilization);
        System.out.printf("Throughput: %.2f p/sec\n", throughput);
        System.out.printf("Average Ready Queue Length: %.2f\n", avg_ready_queue_length);
        System.out.println();
    }
}
