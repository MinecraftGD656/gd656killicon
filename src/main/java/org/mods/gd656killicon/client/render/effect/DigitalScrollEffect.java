package org.mods.gd656killicon.client.render.effect;

import net.minecraft.util.Mth;

/**
 * Digital scroll animation effect for text/number display.
 * Provides smooth scrolling animation from a start value to a target value
 * with configurable duration, refresh rate, and easing functions.
 * Each instance maintains its own animation state.
 */
public class DigitalScrollEffect {
    
    /**
     * Predefined easing functions for animation.
     */
    public enum Easing {
        /** Quintic ease-out: 1 - (1-t)^5 (used by ScoreSubtitleRenderer) */
        QUINTIC_OUT,
        /** Cubic ease-out: 1 - (1-t)^3 (used by BonusListRenderer) */
        CUBIC_OUT,
        /** Linear interpolation */
        LINEAR
    }
    
    private float animationDuration = 1.25f;     private float animationRefreshRate = 0.01f;     private Easing easing = Easing.QUINTIC_OUT;
    
    private float startValue = 0.0f;
    private float targetValue = 0.0f;
    private float currentValue = 0.0f;
    private long startTime = -1;
    private long lastUpdateTime = -1;
    private boolean isAnimating = false;
    
    /**
     * Default constructor with default configuration.
     */
    public DigitalScrollEffect() {}
    
    /**
     * Constructor with custom configuration.
     * @param animationDuration Animation duration in seconds
     * @param animationRefreshRate Minimum time between updates in seconds
     * @param easing Easing function to use
     */
    public DigitalScrollEffect(float animationDuration, float animationRefreshRate, Easing easing) {
        this.animationDuration = animationDuration;
        this.animationRefreshRate = animationRefreshRate;
        this.easing = easing;
    }
    
    
    /**
     * Sets the animation duration in seconds.
     * @param duration Animation duration in seconds (must be > 0)
     */
    public void setAnimationDuration(float duration) {
        if (duration > 0) {
            this.animationDuration = duration;
        }
    }
    
    /**
     * Sets the minimum time between animation updates in seconds.
     * @param refreshRate Minimum time between updates in seconds (must be > 0)
     */
    public void setAnimationRefreshRate(float refreshRate) {
        if (refreshRate > 0) {
            this.animationRefreshRate = refreshRate;
        }
    }
    
    /**
     * Sets the easing function to use for animation.
     * @param easing Easing function
     */
    public void setEasing(Easing easing) {
        this.easing = easing;
    }
    
    /**
     * Gets the current animation duration.
     * @return Animation duration in seconds
     */
    public float getAnimationDuration() {
        return animationDuration;
    }
    
    /**
     * Gets the current animation refresh rate.
     * @return Minimum time between updates in seconds
     */
    public float getAnimationRefreshRate() {
        return animationRefreshRate;
    }
    
    /**
     * Gets the current easing function.
     * @return Easing function
     */
    public Easing getEasing() {
        return easing;
    }
    
    
    /**
     * Starts a new animation from the current value to the target value.
     * @param targetValue Target value to animate to
     */
    public void startAnimation(float targetValue) {
        this.startAnimation(this.currentValue, targetValue);
    }
    
    /**
     * Starts a new animation from a specific start value to a target value.
     * @param startValue Starting value for animation
     * @param targetValue Target value to animate to
     */
    public void startAnimation(float startValue, float targetValue) {
        this.startValue = startValue;
        this.targetValue = targetValue;
        this.currentValue = startValue;
        this.startTime = System.currentTimeMillis();
        this.lastUpdateTime = startTime;
        this.isAnimating = true;
    }
    
    /**
     * Stops any ongoing animation and sets the current value to the target value.
     */
    public void stopAnimation() {
        if (isAnimating) {
            this.currentValue = this.targetValue;
            this.isAnimating = false;
            this.startTime = -1;
            this.lastUpdateTime = -1;
        }
    }
    
    /**
     * Checks if an animation is currently in progress.
     * @return true if animation is active, false otherwise
     */
    public boolean isAnimating() {
        return isAnimating;
    }
    
    /**
     * Gets the current animated value.
     * This value is updated when {@link #update(long)} is called.
     * @return Current animated value
     */
    public float getCurrentValue() {
        return currentValue;
    }
    
    /**
     * Gets the target value of the current animation.
     * @return Target value
     */
    public float getTargetValue() {
        return targetValue;
    }
    
    /**
     * Gets the starting value of the current animation.
     * @return Starting value
     */
    public float getStartValue() {
        return startValue;
    }
    
    
    /**
     * Updates the animation state based on the current time.
     * This should be called each frame or regularly to advance the animation.
     * @param currentTime Current time in milliseconds
     * @return Updated current value
     */
    public float update(long currentTime) {
        if (!isAnimating || startTime < 0 || animationDuration <= 0) {
            return currentValue;
        }
        
        if (lastUpdateTime > 0 && currentTime - lastUpdateTime < animationRefreshRate * 1000) {
            return currentValue;
        }
        
        float elapsedSeconds = (currentTime - startTime) / 1000.0f;
        float progress = Math.min(elapsedSeconds / animationDuration, 1.0f);
        
        float easedProgress = applyEasing(progress);
        
        currentValue = Mth.lerp(easedProgress, startValue, targetValue);
        
        if (progress >= 1.0f) {
            currentValue = targetValue;
            isAnimating = false;
        }
        
        lastUpdateTime = currentTime;
        return currentValue;
    }
    
    /**
     * Convenience method that updates using the current system time.
     * @return Updated current value
     */
    public float update() {
        return update(System.currentTimeMillis());
    }
    
    /**
     * Gets the rounded integer value of the current animated value.
     * @return Rounded integer value
     */
    public int getRoundedValue() {
        return Math.round(currentValue);
    }
    
    
    /**
     * Applies the configured easing function to a progress value.
     * @param progress Linear progress value (0 to 1)
     * @return Eased progress value (0 to 1)
     */
    private float applyEasing(float progress) {
        if (progress <= 0.0f) return 0.0f;
        if (progress >= 1.0f) return 1.0f;
        
        switch (easing) {
            case CUBIC_OUT:
                float t = 1.0f - progress;
                return 1.0f - t * t * t;
                
            case LINEAR:
                return progress;
                
            case QUINTIC_OUT:
            default:
                float t2 = 1.0f - progress;
                return 1.0f - t2 * t2 * t2 * t2 * t2;
        }
    }
    
    /**
     * Resets all animation state to initial values.
     */
    public void reset() {
        this.startValue = 0.0f;
        this.targetValue = 0.0f;
        this.currentValue = 0.0f;
        this.startTime = -1;
        this.lastUpdateTime = -1;
        this.isAnimating = false;
    }
}