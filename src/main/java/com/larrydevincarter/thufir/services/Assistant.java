package com.larrydevincarter.thufir.services;


public interface Assistant {

    String chat(String message);

    String chat(String message, double temperature);
}