package com.chadate.tidemaid.api;

import com.chadate.tidemaid.task.TaskTideFishing;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;

/**
 * Tide兼容MOD扩展实现
 */
@LittleMaidExtension
public class TideLittleMaidExtension implements ILittleMaid {

    private static final TaskTideFishing TIDE_FISHING_TASK = new TaskTideFishing();

    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(TIDE_FISHING_TASK);
    }
}