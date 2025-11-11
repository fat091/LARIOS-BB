package com.mycompany.proyectopcypoto2025.graphics;

public interface AnimationBridge {
    void setPhilosophersState(int[] states);
    void setProducerConsumerState(int produced, int consumed, int bufferSize);
    void setBarberState(int waiting, boolean barberBusy);
    void setSmokersState(int[] smokerStates);
    void setReadersWritersState(int readers, boolean writerActive);
    void requestFreezeUI();
    void requestUnfreezeUI();
}
