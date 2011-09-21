package com.mysema.query.group;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.Transformer;
import org.junit.Test;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.commons.lang.IteratorAdapter;
import com.mysema.commons.lang.Pair;
import com.mysema.query.Projectable;
import com.mysema.query.support.AbstractProjectable;
import com.mysema.query.types.ConstructorExpression;
import com.mysema.query.types.Expression;
import com.mysema.query.types.expr.NumberExpression;
import com.mysema.query.types.expr.StringExpression;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

public class GroupByTest {

    private static final NumberExpression<Integer> postId = new NumberPath<Integer>(Integer.class, "postId");

    private static final StringExpression postName = new StringPath("postName");

    private static final NumberExpression<Integer> commentId = new NumberPath<Integer>(Integer.class, "commentId");

    private static final StringExpression commentText = new StringPath("commentText");

    private static final GroupColumnDefinition<Integer, String> constant = new AbstractGroupColumnDefinition<Integer, String>(commentId) {

        @Override
        public GroupColumn<String> createGroupColumn() {
            return new GroupColumn<String>() {

                @Override
                public void add(Object o) {
                }

                @Override
                public String get() {
                    return "constant";
                }
            };
        }
    };
    
    private static final ConstructorExpression<Comment> qComment = 
        new ConstructorExpression<GroupByTest.Comment>(
                GroupByTest.Comment.class, 
                new Class[] { Integer.class, String.class },
                commentId, commentText
        );

    public static class Post {
        public final Integer id;
        public final String name;
        public final Set<Comment> comments;
        public Post(Integer id, String name, Set<Comment> comments) {
            this.id = id;
            this.name = name;
            this.comments = comments;
        }
    }
    
    public static class Comment {
        public final Integer id;
        public final String text;
        public Comment(Integer id, String text) {
            this.id = id;
            this.text = text; 
        }
        public int hashCode() {
            return 31*id.hashCode() + text.hashCode();
        }
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof Comment) {
                Comment other = (Comment) o;
                return this.id.equals(other.id) && this.text.equals(other.text);
            } else {
                return false;
            }
        }
    }

    private static <K, V> Pair<K, V> pair(K key, V value) {
        return new Pair<K, V>(key, value);
    }
    
    private static final Projectable BASIC_RESULTS = projectable(
            row(1, "post 1", 1, "comment 1"),
            row(2, "post 2", 4, "comment 4"),
            row(1, "post 1", 2, "comment 2"),
            row(2, "post 2", 5, "comment 5"),
            row(3, "post 3", 6, "comment 6"),
            row(null, "null post", 7, "comment 7"),
            row(null, "null post", 8, "comment 8"),
            row(1, "post 1", 3, "comment 3")
        );
    
    private static final Projectable MAP_RESULTS = projectable(
            row(1, "post 1", pair(1, "comment 1")),
            row(1, "post 1", pair(2, "comment 2")),
            row(2, "post 2", pair(5, "comment 5")),
            row(3, "post 3", pair(6, "comment 6")),
            row(null, "null post", pair(7, "comment 7")),
            row(null, "null post", pair(8, "comment 8")),
            row(1, "post 1", pair(3, "comment 3"))
    );
    
    private static final Projectable POST_W_COMMENTS_RESULTS = projectable(
            row(1, "post 1", comment(1)),
            row(1, "post 1", comment(2)),
            row(2, "post 2", comment(5)),
            row(3, "post 3", comment(6)),
            row(null, "null post", comment(7)),
            row(null, "null post", comment(8)),
            row(1, "post 1", comment(3))
    );
    
    @Test 
    public void Group_Order() {
        Map<Integer, Group> results = 
            GroupBy.create(postId).withOne(postName).withSet(commentId)
            .transform(BASIC_RESULTS);
                
        assertEquals(4, results.size());
    }
    
    @Test
    public void First_Set_And_List() {
        Map<Integer, Group> results = 
            GroupBy.create(postId).withOne(postName).withSet(commentId).withList(commentText)
            .transform(BASIC_RESULTS);

        Group group = results.get(1);
        assertEquals(toInt(1), group.getOne(postId));
        assertEquals("post 1", group.getOne(postName));
        assertEquals(toSet(1, 2, 3), group.getSet(commentId));
        assertEquals(Arrays.asList("comment 1", "comment 2", "comment 3"), group.getList(commentText));
    }
    
    @Test
    public void Group_By_Null() {
        Map<Integer, Group> results = 
            GroupBy.create(postId).withOne(postName).withSet(commentId).withList(commentText)
            .transform(BASIC_RESULTS);

        Group group = results.get(null);
        assertNull(group.getOne(postId));
        assertEquals("null post", group.getOne(postName));
        assertEquals(toSet(7, 8), group.getSet(commentId));
        assertEquals(Arrays.asList("comment 7", "comment 8"), group.getList(commentText));
    }
    
    @Test
    public void With_Constant_Column() {
        Map<Integer, Group> results = 
            GroupBy.create(postId).withOne(postName).withGroup(constant)
            .transform(BASIC_RESULTS);
        
        Group group = results.get(1);
        assertEquals("constant", group.getGroup(constant));
    }
    
    @Test
    public void Map() {
        Map<Integer, Group> results = 
            GroupBy.create(postId).withOne(postName).withMap(commentId, commentText)
            .transform(MAP_RESULTS);

        Group group = results.get(1);
        
        Map<Integer, String> comments = group.getMap(commentId, commentText);
        assertEquals(3, comments.size());
        assertEquals("comment 2", comments.get(2));
    }

    @Test
    public void Array_Access() {
        Map<Integer, Group> results = 
            GroupBy.create(postId).withOne(postName).withSet(commentId).withList(commentText)
            .transform(BASIC_RESULTS);

        Group group = results.get(1);
        Object[] array = group.toArray();
        assertEquals(toInt(1), array[0]);
        assertEquals("post 1", array[1]);
        assertEquals(toSet(1, 2, 3), array[2]);
        assertEquals(Arrays.asList("comment 1", "comment 2", "comment 3"), array[3]);
    }
    
    @Test
    public void Transform_Results() {
        Map<Integer, Post> results = 
            GroupBy.create(postId, postName).withSet(qComment)

            .withTransformer(new Transformer<Group, Post>() {
                public Post transform(Group group) {
                    return new Post(group.getOne(postId), group.getOne(postName), group.getSet(qComment));
                }
            })
            
            .transform(POST_W_COMMENTS_RESULTS);

        Post post = results.get(1);
        assertNotNull(post);
        assertEquals(toInt(1), post.id);
        assertEquals("post 1", post.name);
        assertEquals(toSet(comment(1), comment(2), comment(3)), post.comments);
    }
    
    private static Comment comment(Integer id) {
        return new Comment(id, "comment " + id);
    }
    
    private static Projectable projectable(final Object[]... rows) {
        return new AbstractProjectable(){
            public CloseableIterator<Object[]> iterate(Expression<?>[] args) {
                return iterator(rows);
            }
        };
    }
    
    private Integer toInt(int i) {
        return Integer.valueOf(i);
    }
    
    private <T >Set<T> toSet(T... s) {
        return new HashSet<T>(Arrays.asList(s));
    }
    
    private static Object[] row(Object... row) {
        return row;
    }
    
    private static CloseableIterator<Object[]> iterator(Object[]... rows) {
        return new IteratorAdapter<Object[]>(Arrays.asList(rows).iterator());
    }
}
