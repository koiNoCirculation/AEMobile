package dev.youtiao.aemobile.web.service;

import com.google.common.util.concurrent.AtomicDouble;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

@Service("MSPTReal")
public class MSPTRealImpl implements MSPTService {
    double s1 = 0;
    double s2 = 0;
    double s3 = 0;


    ArrayDeque<Double> q1 = new ArrayDeque<>();
    ArrayDeque<Double> q2 = new ArrayDeque<>();
    ArrayDeque<Double> q3 = new ArrayDeque<>();
    static final int TICK5S = 100;
    static final int TICK30S = 600;
    static final int TICK120S = 2400;
    public void addTickms(double tickms) {
        q1.add(tickms);
        if (q1.size() <= TICK5S) {
            s1+=tickms;
        } else {
            s1 += (-q1.pollFirst() + tickms);
        }
        q2.add( tickms);
        if (q2.size() <= TICK30S) {
            s2 += tickms;
        } else {
            s2 += (-q2.pollFirst() + tickms);
        }
        q3.add(tickms);
        if (q3.size() <= TICK120S) {
            s3 += tickms;
        } else {
            s3 += (-q3.pollFirst() + tickms);
        }
    }


    @Override
    public float[] getServerMSPT() {
        return new float[] {(float)s1 / TICK5S, (float)s2 / TICK30S, (float)s3 / TICK120S};
    }
}
