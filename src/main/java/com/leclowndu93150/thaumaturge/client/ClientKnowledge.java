package com.leclowndu93150.thaumaturge.client;

import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;

public final class ClientKnowledge {
    private static PlayerKnowledge knowledge = new PlayerKnowledge();

    private ClientKnowledge() {}

    public static void apply(PlayerKnowledge incoming) {
        knowledge = incoming;
    }

    public static PlayerKnowledge get() {
        return knowledge;
    }
}
