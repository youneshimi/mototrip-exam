package com.mototrip;

import com.mototrip.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void addPointsAddsCorrectly() {
        User user = new User("Younes", false);

        user.addPoints(10);
        user.addPoints(5);

        assertEquals(15, user.getPoints());
    }

    @Test
    void canJoinPremiumReturnsTrueWhenPremium() {
        User user = new User("Younes", true);

        assertTrue(user.canJoinPremium());
    }

    @Test
    void canJoinPremiumReturnsFalseWhenNotPremium() {
        User user = new User("Younes", false);

        assertFalse(user.canJoinPremium());
    }
}

