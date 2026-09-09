# 分类白名单 + 两级渲染 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 补齐分类两级结构（`type_pid`），实现「白名单过滤 + 父级分组渲染」，让首页只显示用户词表中的分类、搜索过滤掉敏感片源。

**架构：** `Category.typePid` 由 `MaccmsJson.categories()` 解析；新增纯函数 `CategoryFilter`（`isAllowed` + `buildGroups` + 默认词表）与薄持久化 `CategoryBlockerRepo`（SharedPreferences）；`Categories` 首页改成分组渲染，`SearchScreen` 逐页过滤并在整页滤空时顺延下一页。JS 蜘蛛复用 `MaccmsJson` 自动受益。

**技术栈：** Kotlin/Java + Jetpack Compose（现有工程）。测试仅 JUnit4 纯 JVM（**无 Robolectric**，`CategoryBlockerRepo` 与 UI 不做单测，与现有 `SourceRepo` 一致，由构建验证覆盖）。

---

### 任务 1：`Category.typePid` 与 `MaccmsJson` 解析

**文件：**
- 修改：`app/src/main/java/com/chuichui/video/bean/Category.java`
- 修改：`app/src/main/java/com/chuichui/video/api/MaccmsJson.java:21-29`
- 测试：`app/src/test/java/com/chuichui/video/MaccmsJsonTest.java`

- [ ] **步骤 1：编写失败的测试**

编辑 `app/src/test/java/com/chuichui/video/MaccmsJsonTest.java`：在 `parsesCategories()` 内追加断言并新增一个带 pid 的用例。

```java
    @Test
    public void parsesCategoriesWithTypePid() {
        String json = "{\"class\":["
                + "{\"type_id\":\"1\",\"type_name\":\"电影\"},"
                + "{\"type_id\":\"7\",\"type_name\":\"动作片\",\"type_pid\":\"1\"},"
                + "{\"type_id\":\"9\",\"type_name\":\"成人\",\"type_pid\":\"0\"}]}";
        List<Category> out = MaccmsJson.categories(JsonParser.parseString(json).getAsJsonObject());
        assertEquals(3, out.size());
        assertEquals("电影", out.get(0).typeName);
        assertEquals(0L, out.get(0).typePid);
        assertEquals("动作片", out.get(1).typeName);
        assertEquals(1L, out.get(1).typePid);
        assertEquals("成人", out.get(2).typeName);
        assertEquals(0L, out.get(2).typePid);
    }
```

同时在现有 `parsesCategories()` 末尾加：`assertEquals(0L, out.get(0).typePid);`（CLASS_JSON 无 `type_pid` → 默认 0）。

- [ ] **步骤 2：运行测试验证失败**

运行：`.\gradlew.bat testMobileDebugUnitTest --tests "*MaccmsJsonTest*"`（JAVA_HOME 需为 `C:\Program Files\Java\jdk-21`）
预期：编译失败 `cannot find symbol: field typePid`。

- [ ] **步骤 3：编写实现代码**

编辑 `Category.java`，在 `typeName` 后加字段：

```java
    public String typeName;
    /** 父分类 id（苹果 CMS class[].type_pid）；无 pid 的源（平铺）为 0。 */
    public long typePid;
```

编辑 `MaccmsJson.categories()`：

```java
    public static List<Category> categories(JsonObject obj) {
        List<Category> out = new ArrayList<>();
        JsonArray a = obj.has("class") ? obj.getAsJsonArray("class") : new JsonArray();
        for (JsonElement e : a) {
            JsonObject c = e.getAsJsonObject();
            Category cat = new Category(str(c, "type_id"), str(c, "type_name"));
            cat.typePid = pidOf(c);
            out.add(cat);
        }
        return out;
    }

    /** type_pid 解析：缺省/null/非数字一律 0。 */
    private static long pidOf(JsonObject o) {
        if (!o.has("type_pid") || o.get("type_pid").isJsonNull()) return 0L;
        try {
            return Long.parseLong(o.get("type_pid").getAsString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
```

- [ ] **步骤 4：运行测试验证通过**

运行：`.\gradlew.bat testMobileDebugUnitTest --tests "*MaccmsJsonTest*"`
预期：PASS，所有 MaccmsJson 用例通过。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/chuichui/video/bean/Category.java app/src/main/java/com/chuichui/video/api/MaccmsJson.java app/src/test/java/com/chuichui/video/MaccmsJsonTest.java
git commit -m "feat(category): parse type_pid into Category"
```

---

### 任务 2：`CategoryFilter` 纯函数（白名单匹配 + 分组析构）

**文件：**
- 创建：`app/src/main/java/com/chuichui/video/category/CategoryGroup.java`
- 创建：`app/src/main/java/com/chuichui/video/category/CategoryFilter.java`
- 测试：`app/src/test/java/com/chuichui/video/CategoryFilterTest.java`

- [ ] **步骤 1：编写失败的测试**

创建 `app/src/test/java/com/chuichui/video/CategoryFilterTest.java`（包 `com.chuichui.video`）：

```java
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
        assertTrue(CategoryFilter.isAllowed("动作片", Arrays.asList("动作"))); // typeName 包含词表子词即命中
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
```

- [ ] **步骤 2：运行测试验证失败**

运行：`.\gradlew.bat testMobileDebugUnitTest --tests "*CategoryFilterTest*"`
预期：编译失败 `cannot find symbol: class CategoryFilter`。

- [ ] **步骤 3：编写实现代码**

创建 `app/src/main/java/com/chuichui/video/category/CategoryGroup.java`：

```java
package com.chuichui.video.category;

import com.chuichui.video.bean.Category;

import java.util.List;

/** 首页分类的一个渲染分组：title 为空表示「无分组标题」（平铺源 / 孤立分类）。 */
public class CategoryGroup {
    public String title;
    public List<Category> categories;

    public CategoryGroup(String title, List<Category> categories) {
        this.title = title;
        this.categories = categories;
    }
}
```

创建 `app/src/main/java/com/chuichui/video/category/CategoryFilter.java`：

```java
package com.chuichui.video.category;

import com.chuichui.video.bean.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 分类白名单过滤 + 两级分组析构（纯函数，无 Android 依赖）。
 *
 * 匹配规则：typeName（忽略大小写）包含任一非空白名单词即放行；typeName 为空放行；
 * 词表为空 → 全隐藏；词表为 null → 全放行。
 *
 * 分组规则：
 * - 无 pid 结构的源（全部 typePid==0，如天涯）→ 平铺单组（title=null），逐分类过白名单；
 * - 有 pid 结构的源 → 叶子（typePid>0）按父级分组（组标题=父分类名）；
 *   叶子**仅按自身 typeName** 命中白名单即放行（父级不参与叶子匹配，避免词表命中父级时放行其下敏感分类）；
 *   父级无放行叶子则整组剔除；
 *   父亲不在列表的叶子、以及有 pid 结构中孤立的 pid==0 分类 → 归入若干 null 标题组放在最后（按 raw 顺序）。
 */
public final class CategoryFilter {

    private CategoryFilter() {
    }

    /** 内置默认白名单：一级兼容词（供平铺源/父级匹配）+ 常见影视叶子类目。 */
    public static final List<String> DEFAULT_TERMS = Arrays.asList(
            "电影", "电视剧", "动漫", "综艺",
            "动作片", "喜剧片", "爱情片", "科幻片", "奇幻片", "冒险片",
            "恐怖片", "惊悚片", "悬疑片", "犯罪片", "剧情片", "战争片",
            "纪录片", "动画片", "武侠片", "灾难片",
            "大陆剧", "国产剧", "港剧", "台剧", "韩剧", "日剧", "美剧",
            "英剧", "泰剧", "海外剧", "日漫", "国漫", "少儿");

    public static boolean isAllowed(String typeName, List<String> terms) {
        if (typeName == null) return true;
        if (terms == null) return true;
        if (typeName.isEmpty()) return true;
        if (terms.isEmpty()) return false;
        String t = typeName.toLowerCase(Locale.ROOT).trim();
        for (String term : terms) {
            if (term == null) continue;
            String tr = term.trim().toLowerCase(Locale.ROOT);
            if (!tr.isEmpty() && t.contains(tr)) return true;
        }
        return false;
    }

    /** 分类 → 渲染分组（顺序稳定、空组已剔除、孤立归尾部）。空词表返回空列表。 */
    public static List<CategoryGroup> buildGroups(List<Category> raw, List<String> allowTerms) {
        if (raw == null || raw.isEmpty()) return new ArrayList<>();

        boolean tree = false;
        for (Category c : raw) {
            if (c.typePid > 0) {
                tree = true;
                break;
            }
        }

        if (!tree) {
            List<Category> ok = new ArrayList<>();
            for (Category c : raw) if (isAllowed(c.typeName, allowTerms)) ok.add(c);
            if (ok.isEmpty()) return new ArrayList<>();
            List<CategoryGroup> flat = new ArrayList<>();
            flat.add(new CategoryGroup(null, ok));
            return flat;
        }

        Map<String, Category> byId = new HashMap<>();
        for (Category c : raw) byId.put(c.typeId, c);
        Set<String> childPids = new HashSet<>();
        for (Category c : raw) if (c.typePid > 0) childPids.add(c.typePid);

        Map<String, CategoryGroup> byGroup = new LinkedHashMap<>(); // key = 父 typeId
        List<Category> isolated = new ArrayList<>();
        for (Category c : raw) {
            if (c.typePid > 0) {
                Category parent = byId.get(c.typePid);
                boolean allow = isAllowed(c.typeName, allowTerms);
                if (!allow) continue;
                if (parent == null) {
                    isolated.add(c);
                    continue;
                }
                CategoryGroup g = byGroup.get(parent.typeId);
                if (g == null) {
                    g = new CategoryGroup(parent.typeName, new ArrayList<>());
                    byGroup.put(parent.typeId, g);
                }
                g.categories.add(c);
            } else {
                if (!childPids.contains(c.typeId) && isAllowed(c.typeName, allowTerms)) isolated.add(c);
            }
        }

        List<CategoryGroup> out = new ArrayList<>();
        for (CategoryGroup g : byGroup.values()) if (!g.categories.isEmpty()) out.add(g);
        if (!isolated.isEmpty()) out.add(new CategoryGroup(null, isolated));
        return out;
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`.\gradlew.bat testMobileDebugUnitTest --tests "*CategoryFilterTest*"`
预期：PASS，全部 8 个用例通过。

- [ ] **步骤 5：Commit**

```bash
git add app/src/main/java/com/chuichui/video/category/ app/src/test/java/com/chuichui/video/CategoryFilterTest.java
git commit -m "feat(category): CategoryFilter whitelist matching and group building"
```

---

### 任务 3：`CategoryBlockerRepo` 持久化

**文件：**
- 创建：`app/src/main/java/com/chuichui/video/category/CategoryBlockerRepo.java`

- [ ] **步骤 1：编写实现代码**

创建 `app/src/main/java/com/chuichui/video/category/CategoryBlockerRepo.java`：

```java
package com.chuichui.video.category;

import android.content.Context;

import com.chuichui.video.data.JsonPref;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/** 分类白名单仓库：本地持久化用户词表；未保存过时返回内置默认词表。 */
public class CategoryBlockerRepo {

    private final JsonPref<List<String>> pref;

    public CategoryBlockerRepo(Context c) {
        pref = new JsonPref<>(c, "chui_cat", "allowTerms", new TypeToken<List<String>>() {});
    }

    /** 当前生效词表；从未保存过 → 内置默认词表（等价「恢复默认」）。 */
    public synchronized List<String> load() {
        List<String> v = pref.get();
        return v != null ? new ArrayList<>(v) : new ArrayList<>(CategoryFilter.DEFAULT_TERMS);
    }

    /** 保存词表：null 不写（防脏数据）；空列表照常写入（=「清空全部 → 全隐藏」的合法语义）。 */
    public synchronized void save(List<String> terms) {
        if (terms == null) return;
        pref.set(terms);
    }
}
```

- [ ] **步骤 2：构建验证通过**

运行：`.\gradlew.bat assembleMobileDebug assembleLeanbackDebug`
预期：BUILD SUCCESSFUL。

- [ ] **步骤 3：Commit**

```bash
git add app/src/main/java/com/chuichui/video/category/CategoryBlockerRepo.java
git commit -m "feat(category): CategoryBlockerRepo persistence"
```

---

### 任务 4：首页 `Categories` 两级分组渲染 + 白名单

**文件：**
- 修改：`app/src/main/java/com/chuichui/video/ui/Categories.kt`

- [ ] **步骤 1：编写实现代码**

编辑 `Categories.kt`：

1) 导入区追加：

```kotlin
import com.chuichui.video.category.CategoryBlockerRepo
import com.chuichui.video.category.CategoryFilter
import com.chuichui.video.category.CategoryGroup
```

2) 状态（`categories` 保留，新增 `groups`）：

```kotlin
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var groups by remember { mutableStateOf<List<CategoryGroup>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
```

3) `LaunchedEffect(selectedId)` 中加载后分组：

```kotlin
    LaunchedEffect(selectedId) {
        sources = repo.load()
        if (sources.none { it.id == selectedId }) selectedId = sources.firstOrNull()?.id ?: ""
        repo.setSelectedId(selectedId)
        loading = true
        categories = loadFromSource(ctx) { adapter -> adapter.home() }
        groups = CategoryFilter.buildGroups(categories, CategoryBlockerRepo(ctx).load())
        loading = false
    }
```

4) `when` 分支的「分类网格」部分替换为分组渲染（原 `else -> LazyVerticalGrid { items(categories)… }` 整体替换）：

```kotlin
            else -> {
                if (categories.isNotEmpty() && groups.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "已屏蔽全部敏感分类\n可在「源」→「分类屏蔽」调整",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 128.dp),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        groups.forEach { g ->
                            if (g.title != null) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        g.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
                                    )
                                }
                            }
                            items(g.categories, key = { it.typeId }) { c ->
                                CategoryTile(c.typeName) { onOpenCategory(c.typeId, c.typeName) }
                            }
                        }
                    }
                }
            }
```

5) 新增 import：`androidx.compose.foundation.lazy.grid.GridItemSpan`。

- [ ] **步骤 2：构建验证通过**

运行：`.\gradlew.bat assembleMobileDebug assembleLeanbackDebug`
预期：BUILD SUCCESSFUL。

- [ ] **步骤 3：Commit**

```bash
git add app/src/main/java/com/chuichui/video/ui/Categories.kt
git commit -m "feat(category): group-home categories by parent and apply whitelist"
```

---

### 任务 5：搜索过滤 + 整页滤空自动顺延

**文件：**
- 修改：`app/src/main/java/com/chuichui/video/ui/SearchScreen.kt`

- [ ] **步骤 1：编写实现代码**

编辑 `SearchScreen.kt`：

1) imports 追加：

```kotlin
import androidx.compose.runtime.getValue   // 已有，见下说明
import com.chuichui.video.bean.Vod
import com.chuichui.video.category.CategoryBlockerRepo
import com.chuichui.video.category.CategoryFilter
```

2) 状态区加 terms（`hasSource` 附近）：

```kotlin
    val hasSource = remember { SourceRepo(ctx).load().isNotEmpty() }
    val allowTerms = remember { CategoryBlockerRepo(ctx).load() }
```

3) 新增私有加载函数（放在 `runSearch` 前）：

```kotlin
    /** 加载某次搜索的一个「有效页」：逐页过滤白名单，整页滤空则顺延下一页（到 pageCount 为止）。
     *  返回 (过滤后片源, 是否还有更多页, 实际使用的末页号)。 */
    suspend fun loadFiltered(kw: String, startPage: Int): Triple<List<Vod>, Boolean, Int> {
        var pg = startPage
        while (true) {
            val r = loadVodPageFromSource(ctx, pg) { adapter -> adapter.search(kw, pg) }
            val kept = r.vods.filter { CategoryFilter.isAllowed(it.typeName, allowTerms) }
            val more = r.hasMore(pg)
            if (kept.isNotEmpty() || !more) return Triple(kept, more, pg)
            pg++
        }
    }
```

4) `runSearch` 替换为：

```kotlin
    fun runSearch() {
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        searchJob?.cancel()
        loading = true
        searchJob = scope.launch {
            val (list, more, lastPg) = loadFiltered(kw, 1)
            vods = list
            page = lastPg
            hasMore = more
            loading = false
            searched = true
        }
    }
```

5) `loadNext` 替换为：

```kotlin
    fun loadNext() {
        if (loading || !hasMore) return
        val kw = keyword.trim()
        if (kw.isEmpty()) return
        loading = true
        val next = page + 1
        searchJob = scope.launch {
            val (list, more, lastPg) = loadFiltered(kw, next)
            vods = vods + list
            page = lastPg
            hasMore = more
            loading = false
        }
    }
```

> 说明：`hasMore`/`loading`/`page`/`vods`/`searchJob`/`loadVodPageFromSource` 均为既有成员，不改类型；`kotlinx.coroutines.Job` 已导入。

- [ ] **步骤 2：构建验证通过**

运行：`.\gradlew.bat assembleMobileDebug assembleLeanbackDebug`
预期：BUILD SUCCESSFUL。

- [ ] **步骤 3：Commit**

```bash
git add app/src/main/java/com/chuichui/video/ui/SearchScreen.kt
git commit -m "feat(category): filter search results and skip fully-blocked pages"
```

---

### 任务 6：源管理「分类屏蔽」对话框

**文件：**
- 修改：`app/src/main/java/com/chuichui/video/ui/SourcesScreen.kt`

- [ ] **步骤 1：编写实现代码**

编辑 `SourcesScreen.kt`：

1) imports 追加：

```kotlin
import com.chuichui.video.category.CategoryBlockerRepo
import com.chuichui.video.category.CategoryFilter
```

2) 状态区加：

```kotlin
    var showAllowTerms by remember { mutableStateOf(false) }
```

3) 顶栏行（`Row(verticalAlignment …)` 内，在「批量导入」前插入「分类屏蔽」）：

```kotlin
            Text(
                "分类屏蔽",
                modifier = Modifier.clickable { showAllowTerms = true }.padding(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "批量导入",
                modifier = Modifier.clickable { showImport = true }.padding(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
```

4) 在 `if (showImport) { BatchImportDialog… }` 后追加：

```kotlin
    if (showAllowTerms) {
        AllowTermsDialog(
            initial = remember { CategoryBlockerRepo(ctx).load() },
            onDismiss = { showAllowTerms = false },
            onSaved = { terms ->
                CategoryBlockerRepo(ctx).save(terms)
                showAllowTerms = false
            },
        )
    }
```

5) 文件末尾追加对话框 composable：

```kotlin
/** 分类屏蔽（白名单）编辑对话框：多行文本框每行一词 + 恢复默认。 */
@Composable
private fun AllowTermsDialog(
    initial: List<String>,
    onDismiss: () -> Unit,
    onSaved: (List<String>) -> Unit,
) {
    var text by remember { mutableStateOf(initial.joinToString("\n")) }
    var warnEmpty by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分类屏蔽（白名单）") },
        text = {
            Column {
                Text(
                    "每行一个分类词，只显示命中这些词的分类；空词表将隐藏全部分类。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; if (warnEmpty && it.isNotBlank()) warnEmpty = false },
                    label = { Text("允许的分类词") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (warnEmpty) {
                    Text(
                        "词表为空将隐藏全部分类",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val terms = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (terms.isEmpty()) warnEmpty = true
                onSaved(terms) // 空列表照常保存 = 「清空全部 → 全隐藏」（与任务 3 repo 语义一致）；红色提示仅警示、不拦截
            }) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    text = CategoryFilter.DEFAULT_TERMS.joinToString("\n")
                    warnEmpty = false
                }) { Text("恢复默认") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
```

> 依赖已存在：`AlertDialog`、`OutlinedTextField`、`TextButton`、`Color`、`Row`、`Column`、`fillMaxWidth`、`MaterialTheme`、`remember`、`getValue`/`setValue` 均已由该文件导入。

- [ ] **步骤 2：构建验证通过**

运行：`.\gradlew.bat assembleMobileDebug assembleLeanbackDebug`
预期：BUILD SUCCESSFUL。

- [ ] **步骤 3：Commit**

```bash
git add app/src/main/java/com/chuichui/video/ui/SourcesScreen.kt
git commit -m "feat(category): allow-terms editing dialog in source management"
```

---

### 任务 7：全量回归验证

**文件：** 无

- [ ] **步骤 1：全量构建 + 单测**

运行：`.\gradlew.bat assembleMobileDebug assembleLeanbackDebug testMobileDebugUnitTest`
预期：BUILD SUCCESSFUL，所有既有测试 + 新增 `CategoryFilterTest`/`MaccmsJsonTest` 全绿。

- [ ] **步骤 2：手动明证**

在任意一端（推荐模拟器 mobile debug）真机验证：
1. 首页分类网格：有 pid 源显示「父级标题 + 组内分类」；敏感/杂项分类（如「成人」「直播」）不出现；
2. 点进一个放行分类 → 有片源；
3. 搜索一个命中敏感分类的词 → 敏感片源不出现；若整页被滤空，后续页内容正常显示；
4. 源管理 →「分类屏蔽」→ 空保存出红色提示；恢复默认生效。

- [ ] **步骤 3：文档同步**

编辑 `.scratch/category-filter/spec.md`，在末尾「字段/接口备忘」验证无误后将其标为 `resolved`（若实现与规格有出入，先修正规格再标注）。

```bash
git add .scratch/category-filter/spec.md
git commit -m "docs(category-filter): mark spec resolved"
```