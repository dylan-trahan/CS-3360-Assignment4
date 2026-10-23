
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class Main {

    public static double randExponential(double rate) {
        double u = Math.random();
        return -Math.log(1-u) / rate;
    }

    public static Stats aging(Queue<Event> eq, final int processCount) {
        Stats out = new Stats();

        Comparator<Process> pc = (p1, p2) -> {
            if(p1.priority == p2.priority) return p1.id - p2.id;
            return p1.priority - p2.priority;  
        };

        Queue<Process> rq = new PriorityQueue<>(pc);

        double timePtr = 0.0;
        double max_waiting = 0.0;
        double idle_time = 0.0;
        Event next;
        Process pRunning = null;
        while(!(eq.isEmpty() && rq.isEmpty() && pRunning == null)){
            next = eq.remove();
            //System.out.println(next.toString());
            double dt = next.time - timePtr;
            if(rq.isEmpty() && pRunning == null) idle_time += dt;
            timePtr = next.time;
            if(next.type == EventType.ARR){
                if(pRunning == null){
                    pRunning = next.process;
                    pRunning.start_time = timePtr;
                    Event dep = new Event(EventType.DEP, timePtr + next.process.remaining_service_time, next.process);
                    eq.add(dep);
                }
                else{
                    rq.add(next.process);
                }
            }
            else{
                pRunning.completion_time = timePtr;
                out.avg_turnaround_time += pRunning.completion_time - pRunning.arrival_time;
                out.avg_waiting_time += pRunning.completion_time - pRunning.arrival_time - pRunning.service_time;
                out.avg_response_time += pRunning.start_time - pRunning.arrival_time;

                max_waiting = Math.max(max_waiting, pRunning.completion_time - pRunning.arrival_time);

                if(rq.isEmpty()) pRunning = null;
                else{
                    final double t = timePtr;
                    // Update priorities
                    rq.forEach((p) -> {
                        p.priority = Math.max(1, p.priority - (int)Math.floor((t - p.arrival_time) / 10.0));
                    });
                    pRunning = rq.remove();
                    pRunning.start_time = timePtr;
                    Event dep = new Event(EventType.DEP, timePtr + pRunning.remaining_service_time, pRunning);
                    eq.add(dep);
                }
            }
        }

        out.completion_time = timePtr;
        out.utilization = (timePtr - idle_time) * 100.0 / timePtr;
        out.throughput = processCount / timePtr;
        out.avg_response_time /= (double)processCount;
        out.avg_turnaround_time /= (double)processCount;
        out.avg_waiting_time /= (double)processCount;
        out.avg_ready_queue_length = max_waiting; // Wasn't being used and I got lazy

        return out;
    }

    public static Stats mlfq(Queue<Event> eq, final int processCount) {
        Stats out = new Stats();

        final Queue<Process> rr = new LinkedList<>();
        final Queue<Process> fcfs = new LinkedList<>();

        final double quantum = 2.0;
        final double boost_time = 20.0;
        double max_waiting = 0.0;
        double timePtr = 0.0;
        double idle_time = 0.0;
        Event next;
        Process pRunning = null;
        eq.add(new Event(EventType.BOOST, boost_time, null));
        while(!(eq.isEmpty() && rr.isEmpty() && fcfs.isEmpty() && pRunning == null)){
            next = eq.remove();
            //System.out.println(next.toString());
            if(next.type == EventType.BOOST && eq.isEmpty() && rr.isEmpty() && fcfs.isEmpty() && pRunning == null) break;
            double dt = next.time - timePtr;
            if(rr.isEmpty() && fcfs.isEmpty() && pRunning == null) idle_time += dt;
            timePtr = next.time;

            if(next.type == EventType.BOOST){
                while(!fcfs.isEmpty()) rr.add(fcfs.remove());
                eq.add(new Event(EventType.BOOST, timePtr + boost_time, null));
                continue;
            }

            if(next.type == EventType.ARR){
                if(pRunning == null){
                    pRunning = next.process;
                    pRunning.start_time = timePtr;
                    if(pRunning.remaining_service_time < quantum){
                        eq.add(new Event(EventType.DEP, timePtr + pRunning.remaining_service_time, pRunning));
                    }
                    else eq.add(new Event(EventType.QUANTUM_EXPIRE, timePtr + quantum, pRunning));
                }
                else{
                    rr.add(next.process);
                }
            }

            else if(next.type == EventType.DEP){
                pRunning.completion_time = timePtr;
                out.avg_turnaround_time += pRunning.completion_time - pRunning.arrival_time;
                out.avg_waiting_time += pRunning.completion_time - pRunning.arrival_time - pRunning.service_time;
                out.avg_response_time += pRunning.start_time - pRunning.arrival_time;

                max_waiting = Math.max(max_waiting, pRunning.completion_time - pRunning.arrival_time);

                if(rr.isEmpty() && fcfs.isEmpty()) pRunning = null;
                else{
                    // Choose which queue to take from
                    if(rr.isEmpty()){
                        pRunning = fcfs.remove();
                        Event dep = new Event(EventType.DEP, timePtr + pRunning.remaining_service_time, pRunning);
                        eq.add(dep);
                    } 
                    else{
                        pRunning = rr.remove();
                        if(pRunning.remaining_service_time < quantum){
                            eq.add(new Event(EventType.DEP, timePtr + pRunning.remaining_service_time, pRunning));
                        }
                        else eq.add(new Event(EventType.QUANTUM_EXPIRE, timePtr + quantum, pRunning));
                    } 
                    if(pRunning.start_time < 0) pRunning.start_time = timePtr;
                }
            }

            else{
                pRunning.remaining_service_time -= quantum;
                fcfs.add(pRunning);

                if(rr.isEmpty()){
                    pRunning = fcfs.remove();
                    Event dep = new Event(EventType.DEP, timePtr + pRunning.remaining_service_time, pRunning);
                    eq.add(dep);
                } 
                else{
                    pRunning = rr.remove();
                    if(pRunning.remaining_service_time < quantum){
                        eq.add(new Event(EventType.DEP, timePtr + pRunning.remaining_service_time, pRunning));
                    }
                    else eq.add(new Event(EventType.QUANTUM_EXPIRE, timePtr + quantum, pRunning));
                } 
                if(pRunning.start_time < 0) pRunning.start_time = timePtr;
            }
        }

        out.completion_time = timePtr;
        out.utilization = (timePtr - idle_time) * 100.0 / timePtr;
        out.throughput = processCount / timePtr;
        out.avg_response_time /= (double)processCount;
        out.avg_turnaround_time /= (double)processCount;
        out.avg_waiting_time /= (double)processCount;
        out.avg_ready_queue_length = max_waiting;

        return out;
    }

    public static double[] rm(){
        // Array represents the following metrics
        // [0] = utilization
        // [1] = schedulability bound
        // [2] = all deadlines met (1 if true, 0 otherwise)
        // [3] = deadlines missed
        double[] out = new double[4];

        Task[] tasks = new Task[3];
        tasks[0] = new Task(4, 1);
        tasks[1] = new Task(6, 2);
        tasks[2] = new Task(12, 3);

        Comparator<Task> tc = (t1, t2) -> {
            return t1.period - t2.period;
        };

        Queue<Task> tq = new PriorityQueue<>(tc);

        int idle_time = 0;
        int missed_deadlines = 0;
        Task tRunning = null;
        // obviously not the most efficient to iterate through incrementing time
        // but it works for a small scale like this
        for(int time=0;time<12;time++){
            for(Task t : tasks){
                if(time % t.period == 0){
                    Task tNew = new Task(t.period, t.execution_time);
                    tNew.deadline = time + t.period;
                    tq.add(tNew);
                }
            }

            if(tRunning == null && tq.isEmpty()) idle_time++;
            else{
                if(tRunning == null){
                    tRunning = tq.remove();
                    tRunning.remaining_time--;
                    if(tRunning.remaining_time == 0){
                        if(tRunning.deadline < time) missed_deadlines++;
                        if(tq.isEmpty()) tRunning = null;
                        else tRunning = tq.remove();
                    }
                }
                else{
                    if(!tq.isEmpty() && tq.peek().period < tRunning.period){
                        tq.add(tRunning);
                        tRunning = tq.remove();
                    }
                    tRunning.remaining_time--;
                    if(tRunning.remaining_time == 0){
                        if(tRunning.deadline < time) missed_deadlines++;
                        if(tq.isEmpty()) tRunning = null;
                        else tRunning = tq.remove();
                    }
                }
            }
            
        }

        out[0] = (12-idle_time)/12.0;
        out[1] = 0.693;
        out[2] = (missed_deadlines == 0) ? 1.0 : 0.0;
        out[3] = missed_deadlines;
        
        return out;
    }

    public static double[] edf(){
        // Array represents the following metrics
        // [0] = utilization
        // [1] = schedulability bound
        // [2] = all deadlines met (1 if true, 0 otherwise)
        // [3] = deadlines missed
        double[] out = new double[4];

        Task[] tasks = new Task[3];
        tasks[0] = new Task(4, 1);
        tasks[1] = new Task(6, 2);
        tasks[2] = new Task(12, 3);

        Comparator<Task> tc = (t1, t2) -> {
            return t1.deadline - t2.deadline;
        };

        Queue<Task> tq = new PriorityQueue<>(tc);

        int idle_time = 0;
        int missed_deadlines = 0;
        Task tRunning = null;
        // obviously not the most efficient to iterate through incrementing time
        // but it works for a small scale like this
        for(int time=0;time<12;time++){
            for(Task t : tasks){
                if(time % t.period == 0){
                    Task tNew = new Task(t.period, t.execution_time);
                    tNew.deadline = time + t.period;
                    tq.add(tNew);
                }
            }

            if(tRunning == null && tq.isEmpty()) idle_time++;
            else{
                if(tRunning == null){
                    tRunning = tq.remove();
                    tRunning.remaining_time--;
                    if(tRunning.remaining_time == 0){
                        if(tRunning.deadline < time) missed_deadlines++;
                        if(tq.isEmpty()) tRunning = null;
                        else tRunning = tq.remove();
                    }
                }
                else{
                    if(!tq.isEmpty() && tq.peek().deadline < tRunning.deadline){
                        tq.add(tRunning);
                        tRunning = tq.remove();
                    }
                    tRunning.remaining_time--;
                    if(tRunning.remaining_time == 0){
                        if(tRunning.deadline < time) missed_deadlines++;
                        if(tq.isEmpty()) tRunning = null;
                        else tRunning = tq.remove();
                    }
                }
            }
            
        }

        out[0] = (12-idle_time)/12.0;
        out[1] = 1.0;
        out[2] = (missed_deadlines == 0) ? 1.0 : 0.0;
        out[3] = missed_deadlines;
        
        return out;
    }

    public static void main(String[] args) {
        final int PROCESS_COUNT = 1000;

        // Create comparator to time-sort event queue
        Comparator<Event> ec = (e1, e2) -> {
            return (int)Math.signum(e1.time - e2.time);
        };

        Queue<Event> eq = new PriorityQueue<>(ec);
        
        double aTime = 0.0;
        // Create arrival events to be used in simulations
        for(int i=0;i<PROCESS_COUNT;i++){
            Process p = new Process();
            p.id = i+1;
            double interTime = randExponential(2);
            p.arrival_time = aTime + interTime;
            aTime += interTime;
            p.service_time = randExponential(1);
            p.start_time = -1.0;
            p.remaining_service_time = p.service_time;
            p.priority = (int)Math.floor(Math.random() * 10);
            Event e = new Event(EventType.ARR, p.arrival_time, p);
            eq.add(e);
        }

        Queue<Event> eqCopy;

        eqCopy = new PriorityQueue<>(ec);
        for(Event e : eq) eqCopy.add(e.copy());
        Stats p1_1 = aging(eqCopy, PROCESS_COUNT);

        eqCopy = new PriorityQueue<>(ec);
        for(Event e : eq) eqCopy.add(e.copy());
        Stats p1_2 = mlfq(eqCopy, PROCESS_COUNT);

        double[] p2_1 = rm();
        double[] p2_2 = edf();

        try {
            FileWriter writer = new FileWriter("output.md");
            writer.write("## Part 1: Advanced Schedulers Summary (1,000 Processes)\n\n");
            writer.write("|Metric|Priority w/ Aging|MLFQ (Boost S=20ms)|\n");
            writer.write("|:---:|:---:|:---:|\n");

            writer.write(String.format("|Total Completion Time (ms)|%.2f|%.2f|\n", 
            p1_1.completion_time, p1_2.completion_time));

            writer.write(String.format("|Average Turnaround Time (ms)|%.2f|%.2f|\n", 
            p1_1.avg_turnaround_time, p1_2.avg_turnaround_time));

            writer.write(String.format("|Average Waiting Time (ms)|%.2f|%.2f|\n", 
            p1_1.avg_waiting_time, p1_2.avg_waiting_time));

            writer.write(String.format("|Average Response Time (ms)|%.2f|%.2f|\n", 
            p1_1.avg_response_time, p1_2.avg_response_time));

            writer.write(String.format("|Overall CPU Utilization (%%)|%.2f|%.2f|\n", 
            p1_1.utilization, p1_2.utilization));

            writer.write(String.format("|Maximum Waiting Time (ms)|%.2f|%.2f|\n\n\n", 
            p1_1.avg_ready_queue_length, p1_2.avg_ready_queue_length));

            writer.write("## Part 2: Real-Time Schedulers Summary (Hyper-Period = 12 ms)\n\n");
            writer.write("|Metric|Rate-Monotonic (RM)|Earliest-Deadline-First (EDF)|\n");
            writer.write("|:---:|:---:|:---:|\n");

            writer.write(String.format("|Total Utilization|%.2f|%.2f|\n", p2_1[0], p2_2[0]));
            writer.write(String.format("|Theoretical Schedulability Bound|%.2f|%.2f|\n", p2_1[1], p2_2[1]));
            writer.write(String.format("|All Deadlines Met?|%s|%s|\n", 
            (p2_1[2] == 1.0) ? "Yes" : "No", (p2_2[2] == 1.0) ? "Yes" : "No"));
            writer.write(String.format("|Total Deadline Misses|%.2f|%.2f|\n", p2_1[3], p2_2[3]));

            System.out.println("Output saved to output.md");

            writer.close();
        } catch (IOException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
}