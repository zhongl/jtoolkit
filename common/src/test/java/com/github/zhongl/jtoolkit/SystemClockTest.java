package com.github.zhongl.jtoolkit;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a>
 */
public class SystemClockTest {

    @Test
    public void shouldGetNow() throws Exception {
        long precision = 10L;
        SystemClock clock = new SystemClock(precision);
        assertThat(clock.precision(),is(precision));

        Thread.sleep(precision * 2);

        long nowFromSystem = System.currentTimeMillis();
        long nowFromClock = clock.now();
        assertThat(nowFromClock - nowFromSystem, lessThanOrEqualTo(precision));
    }


}
