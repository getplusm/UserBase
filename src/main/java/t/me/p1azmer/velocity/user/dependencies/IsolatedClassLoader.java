package t.me.p1azmer.velocity.user.dependencies;

import java.net.URL;
import java.net.URLClassLoader;

public class IsolatedClassLoader extends URLClassLoader {

  public IsolatedClassLoader(URL[] urls) {
    super(urls, ClassLoader.getSystemClassLoader().getParent());
  }

  static {
    ClassLoader.registerAsParallelCapable();
  }
}
