package com.buctta.api.serviceimp;

import com.buctta.api.dto.LearningStatsDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成长等级判定。
 * <p>
 * 该规则原先散在前端按笔记/课程数硬折算，现收到后端作为唯一口径，
 * 因此这里用反射覆盖各级边界，确保"达标条件"与文案一致。
 */
class LearningGrowthStageTest {

    private final IMPL_LearningStatsService service = new IMPL_LearningStatsService();

    @Test
    @DisplayName("新用户无任何学习记录时停留在启程阶段")
    void freshStudentStaysAtStart() throws Exception {
        LearningStatsDTO.GrowthStage stage = growth(0, 0, 0);

        assertEquals("start", stage.key());
        assertEquals("启程", stage.title());
        assertEquals(1, stage.achieved());
        assertEquals("稳固", stage.nextTitle());
        assertEquals(0.0, stage.progressToNext(), 1e-9);
    }

    @Test
    @DisplayName("只完成课程但笔记不足时不应升级（条件为『与』而非『或』）")
    void coursesAloneAreNotEnough() throws Exception {
        // 稳固要求：完成 3 门课程 且 3 篇笔记
        LearningStatsDTO.GrowthStage stage = growth(3, 0, 0);

        assertEquals("start", stage.key(), "笔记不足 3 篇不应进入稳固");
        assertEquals(1, stage.achieved());
    }

    @Test
    @DisplayName("完成 3 门课程且 3 篇笔记进入稳固")
    void steadyRequiresBoth() throws Exception {
        LearningStatsDTO.GrowthStage stage = growth(3, 3, 0);

        assertEquals("steady", stage.key());
        assertEquals(2, stage.achieved());
        assertEquals("深耕", stage.nextTitle());
    }

    @Test
    @DisplayName("深耕额外要求 10 次点赞，仅有课程与笔记不足以达标")
    void deepRequiresLikes() throws Exception {
        LearningStatsDTO.GrowthStage noLikes = growth(6, 15, 9);
        assertEquals("steady", noLikes.key(), "点赞 9 次未达 10，不应进入深耕");

        LearningStatsDTO.GrowthStage withLikes = growth(6, 15, 10);
        assertEquals("deep", withLikes.key());
        assertEquals(3, withLikes.achieved());
    }

    @Test
    @DisplayName("完成 10 门课程且 15 篇笔记达到最高阶段领航，无下一阶段")
    void expertIsTopStage() throws Exception {
        LearningStatsDTO.GrowthStage stage = growth(10, 15, 10);

        assertEquals("expert", stage.key());
        assertEquals(4, stage.achieved());
        assertNull(stage.nextTitle(), "已是最高阶段时不应再有下一阶段");
        assertEquals(1.0, stage.progressToNext(), 1e-9);
    }

    @Test
    @DisplayName("超出最高阶段要求仍稳定停在领航，不越界")
    void beyondTopStageStaysAtExpert() throws Exception {
        LearningStatsDTO.GrowthStage stage = growth(999, 999, 999);

        assertEquals("expert", stage.key());
        assertEquals(4, stage.achieved());
    }

    @Test
    @DisplayName("跨级数据直接落到最高达标级（不做逐级累积限制）")
    void skipsToHighestSatisfiedStage() throws Exception {
        // 一次满足领航的全部条件
        LearningStatsDTO.GrowthStage stage = growth(10, 15, 10);

        assertEquals(4, stage.achieved());
    }

    @Test
    @DisplayName("进度取各项条件完成比例的最小值，避免单项达标就显示满格")
    void progressIsBottleneckOfAllConditions() throws Exception {
        // 目标稳固：需 3 门课程 + 3 篇笔记。课程 3/3=1，笔记 1/3≈0.333 → 取 0.333
        LearningStatsDTO.GrowthStage stage = growth(3, 1, 0);

        assertEquals("start", stage.key());
        assertEquals(1.0 / 3, stage.progressToNext(), 1e-9);

        // 课程 1/3≈0.333，笔记 3/3=1 → 同样取课程这一瓶颈
        assertEquals(1.0 / 3, growth(1, 3, 0).progressToNext(), 1e-9);
    }

    @Test
    @DisplayName("深耕阶段的进度把点赞数也计入瓶颈条件")
    void progressForDeepIncludesLikes() throws Exception {
        // 当前稳固，目标深耕：需 6 门课程 + 10 篇笔记 + 10 点赞
        // 课程 3/6=0.5，笔记 15/10→1，点赞 5/10=0.5 → 取 0.5
        LearningStatsDTO.GrowthStage stage = growth(3, 15, 5);

        assertEquals("steady", stage.key());
        assertEquals(0.5, stage.progressToNext(), 1e-9);
    }

    @Test
    @DisplayName("阶段文案与判定条件一致，便于前端直接展示")
    void descriptionsMatchThresholds() throws Exception {
        assertEquals("加入第一门课程", growth(0, 0, 0).description());
        assertEquals("完成 3 门课程并沉淀 3 篇笔记", growth(3, 3, 0).description());
        assertEquals("完成 6 门课程，笔记获 10 次点赞", growth(6, 15, 10).description());
        assertEquals("完成 10 门课程并保持 15 篇以上笔记", growth(10, 15, 10).description());
    }

    private LearningStatsDTO.GrowthStage growth(long completed, long notes, long likes)
            throws Exception {
        Method method = IMPL_LearningStatsService.class
                .getDeclaredMethod("growthStage", long.class, long.class, long.class);
        method.setAccessible(true);
        try {
            return (LearningStatsDTO.GrowthStage) method.invoke(service, completed, notes, likes);
        }
        catch (InvocationTargetException e) {
            throw new AssertionError("growthStage 抛出异常", e.getCause());
        }
    }

    @Test
    @DisplayName("阶段键名稳定，前端可据此选择图标/文案")
    void stageKeysAreStable() throws Exception {
        assertTrue(growth(0, 0, 0).key().equals("start"));
        assertTrue(growth(3, 3, 0).key().equals("steady"));
        assertTrue(growth(6, 15, 10).key().equals("deep"));
        assertTrue(growth(10, 15, 10).key().equals("expert"));
    }
}
