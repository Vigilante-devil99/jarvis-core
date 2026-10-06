package com.jarvis.ai;

import com.jarvis.core.ConversationMemory.Message;
import java.util.List;

public interface ModelClient {
    String generate(String prompt, List<Message> history);
}
