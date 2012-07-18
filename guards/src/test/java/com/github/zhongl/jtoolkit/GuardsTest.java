package com.github.zhongl.jtoolkit;

import org.junit.Test;

import static com.github.zhongl.jtoolkit.Guards.check;
import static com.github.zhongl.jtoolkit.Guards.get;
import static com.github.zhongl.jtoolkit.Validates.Executable;
import static com.github.zhongl.jtoolkit.Validates.expect;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


/**
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a>
 */
public class GuardsTest {

    @Test
    public void shouldGetDefaultValue() throws Exception {
        String defaultValue = "default";
        String value = get((String) null).orDefault(defaultValue);
        assertThat(value, is(defaultValue));
    }

    @Test
    public void shouldGetRealValue() throws Exception {
        String real = "real";
        String value = get(real).orDefault("default");
        assertThat(value, is(real));
    }

    @Test
    public void shouldComplainNotNull() throws Exception {
        final String message = "Should not be null";
        expect(new Executable() {
            @Override
            public void execute() throws Throwable {
                get((String) null).orComplain(message);
            }
        }).complain(IllegalArgumentException.class, message);
    }

    @Test
    public void shouldGetValueWithoutComplain() throws Exception {
        String value = "value";
        assertThat(get(value).orComplain("Should not be null"), is(value));
    }

    @Test
    public void shouldComplainGreaterThanZero() throws Exception {
        expect(new Executable() {
            @Override
            public void execute() throws Throwable {
                check(-1).greaterThan(0).orComplain();
            }
        }).complain(IllegalArgumentException.class, "Number should > 0");
    }

    @Test
    public void shouldNotComplainGreaterThanZero() throws Exception {
        check(1).greaterThan(0).orComplain();
    }

    @Test
    public void shouldComplainGreaterThanOrEqualOne() throws Exception {
        expect(new Executable() {
            @Override
            public void execute() throws Throwable {
                check(0).greaterThanOrEqual(1).orComplain();
            }
        }).complain(IllegalArgumentException.class, "Number should >= 1");
    }

    @Test
    public void shouldNotComplainGreaterThanOrEqualOne() throws Exception {
        check(1).greaterThanOrEqual(1).orComplain();
        check(2).greaterThanOrEqual(1).orComplain();
    }

    @Test
    public void shouldNotComplainLessThanZero() throws Exception {
        check(-1).lessThan(0).orComplain();
    }

    @Test
    public void shouldNotComplainLessThanOrEqualZero() throws Exception {
        check(0).lessThanOrEqual(0).orComplain();
    }

    @Test
    public void shouldComplainLessThanOrEqualZero() throws Exception {
        expect(new Executable() {
            @Override
            public void execute() throws Throwable {
                check(1).lessThanOrEqual(0).orComplain();
            }
        }).complain(IllegalArgumentException.class, "Number should <= 0");
    }

    @Test
    public void shouldComplainNotInRange() throws Exception {
        expect(new Executable() {
            @Override
            public void execute() throws Throwable {
                check(19).greaterThanOrEqual(1).lessThan(18).orComplain();
            }
        }).complain(IllegalArgumentException.class, "Number should < 18");
    }

    @Test
    public void shouldNotComplainInRange() throws Exception {
        check(5).greaterThan(0).lessThanOrEqual(8).orComplain();
    }

    @Test
    public void shouldComplainComplicitLessOperation() throws Exception {
        expect(new Executable() {
            @Override
            public void execute() throws Throwable {
                check(1).lessThan(3).lessThanOrEqual(4).orComplain();
            }
        }).complain(IllegalStateException.class, "Complicit less operation");
    }

    @Test
    public void shouldComplainComplicitGreaterOperation() throws Exception {
        expect(new Executable() {
            @Override
            public void execute() throws Throwable {
                check(1).greaterThanOrEqual(3).greaterThan(4).orComplain();
            }
        }).complain(IllegalStateException.class, "Complicit greater operation");
    }

    @Test
    public void shouldComplainNoOperation() throws Exception {
        expect(new Executable() {
            @Override
            public void execute() throws Throwable {
                check(0).orComplain();
            }
        }).complain(IllegalStateException.class, "No operation");
    }
}
