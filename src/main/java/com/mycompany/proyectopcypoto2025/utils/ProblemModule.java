package com.mycompany.proyectopcypoto2025.utils;

import com.mycompany.proyectopcypoto2025.graphics.AnimationBridge;
import javax.swing.*;

public interface ProblemModule {
    JPanel getPanel();
    void start(String technique, AnimationBridge bridge);
    void stop();
    String getName();
}
