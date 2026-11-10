package com.holub.life;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public interface PatternFormat {
    Set<Point> read(InputStream in) throws IOException;
    void write(Set<Point> cells, OutputStream out) throws IOException;
}
