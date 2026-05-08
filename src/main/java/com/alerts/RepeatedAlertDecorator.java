package com.alerts;

public class RepeatedAlertDecorator extends AlertDecorator{

    private final int  repetitions;
    private final long interval_ms;

    public RepeatedAlertDecorator(Alert wrappedAlert, int repetitions, long interval_ms) {
        super(wrappedAlert);
        this.repetitions = repetitions;
        this.interval_ms = interval_ms;
    }

    @Override
    public String getCondition() {
        return super.getCondition() + " | Repeats " + repetitions + " times every " + interval_ms + "ms";
    }


    public int getRepetitions() {
        return repetitions;
    }

    public long getInterval_ms() {
        return interval_ms;
    }
}
