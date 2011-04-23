package com.github.zhongl.jtoolkit;

import org.junit.Before;
import org.junit.Test;

import static com.github.zhongl.jtoolkit.FileExtendedAttributes.extendedAttributesOfFile;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:zhong.lunfu@gmail.com">zhongl</a>
 */
public class FileExtendedAttributesTest {

  private FileExtendedAttributes fileExtendedAttributes;

  @Before
  public void setUp() throws Exception {
    String path = System.getProperty("file.extended.attributes.path");
    if (path == null) throw new RuntimeException("-Dfile.extended.attributes.path should be set.");
    fileExtendedAttributes = extendedAttributesOfFile(path);
  }

  @Test
  public void setAndGetShort() throws Exception {
    final String name = "replication";
    fileExtendedAttributes.set(name, Short.MAX_VALUE);
    assertThat(fileExtendedAttributes.getShort(name), is(Short.MAX_VALUE));
  }

  @Test
  public void setAndGetInt() throws Exception {
    final String name = "id";
    fileExtendedAttributes.set(name, Integer.MAX_VALUE);
    assertThat(fileExtendedAttributes.getInt(name), is(Integer.MAX_VALUE));
  }

  @Test
  public void setAndGetLong() throws Exception {
    final String name = "blockSize";
    fileExtendedAttributes.set(name, Long.MAX_VALUE);
    assertThat(fileExtendedAttributes.getLong(name), is(Long.MAX_VALUE));
  }

  @Test
  public void setAndGetString() throws Exception {
    final String name = "comment";
    final String value = "no comment";
    fileExtendedAttributes.set(name, value);
    assertThat(fileExtendedAttributes.getString(name, value.getBytes().length), is(value));
  }

}
