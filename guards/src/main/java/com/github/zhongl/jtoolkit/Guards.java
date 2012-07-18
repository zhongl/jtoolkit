package com.github.zhongl.jtoolkit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a>
 */
public final class Guards {

    public static <V> ObjectGet<V> get(V value) {
        return new ObjectGet<V>(value);
    }

    public static NumberCheck check(Number value) {
        return new NumberCheck(value);
    }

    private Guards() {}

    public static class ObjectGet<V> {
        private final V value;

        private ObjectGet(V value) {
            this.value = value;
        }

        public V orDefault(V value) {
            return this.value == null ? value : this.value;
        }

        public V orComplain(String message) {
            if (value == null) throw new IllegalArgumentException(message);
            return value;
        }

    }

    public static class NumberCheck {

        protected final Number value;
        protected final List<Operation> operations = new ArrayList<Operation>();

        NumberCheck(Number value) {
            this.value = value;
        }

        public final NumberCheck greaterThan(Number value) {
            complainIfContainGreater();
            operations.add(new GreaterThan(value));
            return this;
        }

        public final NumberCheck greaterThanOrEqual(Number value) {
            complainIfContainGreater();
            operations.add(new GreaterThanOrEqual(value));
            return this;
        }

        public final NumberCheck lessThan(Number value) {
            complainIfContainLess();
            operations.add(new LessThan(value));
            return this;
        }

        public final NumberCheck lessThanOrEqual(Number value) {
            complainIfContainLess();
            operations.add(new LessThanOrEqual(value));
            return this;
        }

        public final void orComplain() {
            if (operations.isEmpty()) throw new IllegalStateException("No operation");
            for (Operation operation : operations) {
                if (!operation.apply(value)) throw new IllegalArgumentException("Number should " + operation);
            }
        }

        private void complainIfContainGreater() {
            complainIf("Complicit greater operation", GreaterThan.class, GreaterThanOrEqual.class);
        }

        private void complainIfContainLess() {
            complainIf("Complicit less operation", LessThan.class, LessThanOrEqual.class);
        }

        private void complainIf(String reason, Class<?>... classes) {
            for (Operation operation : operations) {
                for (Class<?> aClass : classes) {
                    if (aClass.isInstance(operation)) throw new IllegalStateException(reason);
                }
            }
        }

        private abstract class Operation {
            private final Number value;
            private final String symbol;

            protected Operation(String symbol, Number value) {
                this.symbol = symbol;
                this.value = value;
            }

            final boolean apply(Number base) { return apply(base.doubleValue(), value.doubleValue()); }

            @Override
            public final String toString() { return symbol + " " + value; }

            protected abstract boolean apply(double base, double expect);
        }

        private class GreaterThan extends Operation {
            protected GreaterThan(Number value) { super(">", value); }

            @Override
            protected boolean apply(double base, double expect) { return base > expect; }

        }

        private class GreaterThanOrEqual extends Operation {
            protected GreaterThanOrEqual(Number value) { super(">=", value); }

            @Override
            protected boolean apply(double base, double expect) { return base >= expect; }
        }

        private class LessThan extends Operation {
            protected LessThan(Number value) { super("<", value); }

            @Override
            protected boolean apply(double base, double expect) { return base < expect; }
        }

        private class LessThanOrEqual extends Operation {
            protected LessThanOrEqual(Number value) { super("<=", value); }

            @Override
            protected boolean apply(double base, double expect) { return base <= expect; }
        }
    }
}
