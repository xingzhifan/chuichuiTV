package com.chuichui.video;

import com.chuichui.video.bean.Category;
import com.chuichui.video.category.CategoryFilter;
import com.chuichui.video.category.CategoryGroup;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 分类白名单匹配与两级/平铺分组析构。 */
public class CategoryFilterTest {

    private static Category cat(String id, String name, long pid) {
        Category c = new Category(id, name);
        c.typePid = pid;
        return c;
    }

    private static final List<String> TERMS = Arrays.asList("电影", "电视剧", "动漫", "动作片", "喜剧片", "爱情片");

    @Test
    public void isAllowedBasic() {
        assertTrue(CategoryFilter.isAllowed("动作片", TERMS));
        assertTrue(CategoryFilter.isAllowed("动作片", Arrays.asList("动作")));          // 包含即命中
        assertTrue(CategoryFilter.isAllowed("MARTIAL ARTS", Arrays.asList("martial"))); // 忽略大小写
        assertFalse(CategoryFilter.isAllowed("成人", TERMS));
        assertFalse(CategoryFilter.isAllowed("综艺", TERMS));
    }

    @Test
    public void isAllowedEdgeCases() {
        assertTrue("typeName 为空放行（不误杀）", CategoryFilter.isAllowed("", TERMS));
        assertTrue("typeName null 放行", CategoryFilter.isAllowed(null, TERMS));
        assertTrue("terms null 放行", CategoryFilter.isAllowed("成人", null));
        assertFalse("空词表=全隐藏", CategoryFilter.isAllowed("动作片", Collections.<String>emptyList()));
        assertFalse("全空白词=不命中", CategoryFilter.isAllowed("动作片", Arrays.asList("", "  ")));
    }

    @Test
    public void buildGroupsFlatWhenNoPid() {
        // 无 pid 的源（如天涯）：平铺单组（title=null），按白名单过滤
        List<Category> raw = Arrays.asList(
                cat("1", "电影", 0), cat("2", "电视剧", 0), cat("3", "成人", 0));
        List<CategoryGroup> groups = CategoryFilter.buildGroups(raw, TERMS);
        assertEquals(1, groups.size());
        assertNull(groups.get(0).title);
        assertEquals(2, groups.get(0).categories.size());
        assertEquals("电影", groups.get(0).categories.get(0).typeName);
        assertEquals("电视剧", groups.get(0).categories.get(1).typeName);
    }

    @Test
    public void buildGroupsTreeWhenPidPresent() {
        // 父「电影」下挂动作片（命中，保留）、成人（不在词表，剔除）；孤立 pid=0「综艺」不在词表→剔除
        List<Category> raw = Arrays.asList(
                cat("1", "电影", 0),
                cat("7", "动作片", 1), cat("8", "成人", 1),
                cat("9", "综艺", 0));
        List<CategoryGroup> groups = CategoryFilter.buildGroups(raw, TERMS);
        assertEquals("只剩 电影 一组", 1, groups.size());
        assertEquals("电影", groups.get(0).title);
        assertEquals(1, groups.get(0).categories.size());
        assertEquals("动作片", groups.get(0).categories.get(0).typeName);
    }

    @Test
    public void buildGroupsParentMatchDoesNotAllowLeaf() {
        // 父「电影」命中词表，但叶子「极限运动」未命中 → 叶子被剔除（父级不参与叶子匹配）
        List<Category> raw = Arrays.asList(
                cat("1", "电影", 0),
                cat("7", "极限运动", 1));
        List<CategoryGroup> groups = CategoryFilter.buildGroups(raw, Collections.singletonList("电影"));
        assertTrue(groups.isEmpty());
    }

    @Test
    public void buildGroupsOrphanLeafAndIsolatedGoToNullGroupLast() {
        // 存在的父（电影）有白名单叶子；父不存在的叶子（喜剧片 pid=99）+ 孤立 pid=0（动漫）→ 归 null 组尾部
        List<Category> raw = Arrays.asList(
                cat("1", "电影", 0),
                cat("7", "动作片", 1),
                cat("8", "喜剧片", 99),      // 父 99 不存在
                cat("9", "动漫", 0));        // 孤立 pid=0，命中词表
        List<CategoryGroup> groups = CategoryFilter.buildGroups(raw, TERMS);
        assertEquals(2, groups.size());
        assertEquals("电影", groups.get(0).title);
        assertEquals(1, groups.get(0).categories.size());
        assertEquals("动作片", groups.get(0).categories.get(0).typeName);
        assertNull("孤立组无标题", groups.get(1).title);
        assertEquals(2, groups.get(1).categories.size());
        assertEquals("喜剧片", groups.get(1).categories.get(0).typeName);
        assertEquals("动漫", groups.get(1).categories.get(1).typeName);
    }

    @Test
    public void buildGroupsEmptyTermsHidesAll() {
        List<Category> raw = Arrays.asList(cat("1", "动作片", 0));
        assertTrue(CategoryFilter.buildGroups(raw, Collections.<String>emptyList()).isEmpty());
    }

    @Test
    public void defaultTermsContainBaseCategories() {
        assertTrue(CategoryFilter.DEFAULT_TERMS.contains("电影"));
        assertTrue(CategoryFilter.DEFAULT_TERMS.contains("动作片"));
    }
}
