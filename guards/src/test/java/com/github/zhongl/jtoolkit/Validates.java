package com.github.zhongl.jtoolkit;


import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a>
 */
public class Validates {

    public static ValidateComplain expect(Executable executable) {
        return new ValidateComplain(executable);
    }

    public interface Executable {
        public void execute() throws Throwable;
    }

    public static class ValidateComplain {
        private final Executable executable;

        private ValidateComplain(Executable executable) {this.executable = executable;}

        public void complain(Class<? extends Throwable> klass) {
            complain(klass, null);
        }

        public void complain(Class<? extends Throwable> klass, String message) {
            try {
                executable.execute();
                fail(klass.getName() + " should be thrown.");
            } catch (Throwable t) {
                assertThat(t, instanceOf(klass));
                if (message != null) assertThat(t.getMessage(), is(message));
            }
        }
    }
}
