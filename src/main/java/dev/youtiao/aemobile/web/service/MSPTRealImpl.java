package dev.youtiao.aemobile.web.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

@Service("MSPTReal")
public class MSPTRealImpl implements MSPTService {
    AtomicInteger s1 = new AtomicInteger(0);
    AtomicInteger s2 = new AtomicInteger(0);
    AtomicInteger s3 =  new AtomicInteger(0);


    ArrayDeque<Double> q1 = new ArrayDeque<>();
    AtomicInteger q1l = new AtomicInteger(0);
    ArrayDeque<Double> q2 = new ArrayDeque<>();
    AtomicInteger q2l = new AtomicInteger(0);
    ArrayDeque<Double> q3 = new ArrayDeque<>();
    AtomicInteger q3l = new AtomicInteger(0);
    static final int TICK5S = 300;
    static final int TICK30S = 600;
    static final int TICK120S = 2400;
    public void addTickms(double tickms) {
        if (q1.size() < TICK5S) {
            q1.add( tickms);
            q1l.incrementAndGet();
            s1.accumulateAndGet((int) tickms, Integer::sum);
        } else {
            s1.accumulateAndGet((int) (-q1.pollFirst() + tickms), Integer::sum);
        }
        if (q2.size() < TICK30S) {
            q2.add( tickms);
            q2l.incrementAndGet();
            s2.accumulateAndGet((int) tickms, Integer::sum);
        } else {
            s2.accumulateAndGet((int) (-q2.pollFirst() + tickms), Integer::sum);
        }
        if (q3.size() < TICK120S) {
            q3.add(tickms);
            q3l.incrementAndGet();
            s3.accumulateAndGet((int) tickms, Integer::sum);
        } else {
            s3.accumulateAndGet((int) (-q3.pollFirst() + tickms), Integer::sum);
        }
    }


    @Override
    public float[] getServerMSPT() {
        return new float[] {((float)s1.get()) / TICK5S, ((float)s2.get()) / TICK30S,((float)s3.get()) / TICK120S};
    }
}
