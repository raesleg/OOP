package io.github.raesleg.OOP;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Main extends ApplicationAdapter {

    private EntityManager entityManager;
    private MovementManager movementManager;
    private ShapeRenderer shapes;

    private SimpleMovableEntity player;
    private SimpleMovableEntity ai;

    @Override
    public void create() {
        entityManager = new EntityManager();
        movementManager = new MovementManager();
        shapes = new ShapeRenderer();

        player = new SimpleMovableEntity(200, 200, 220f, new Controls.userControlled());
        ai     = new SimpleMovableEntity(500, 300, 160f, new Controls.AiControlled());

        entityManager.addEntity(player);
        entityManager.addEntity(ai);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        entityManager.update(dt);                  // merges pending entities
        movementManager.update(entityManager, dt);  // calls move(dt) on IMovable

        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.circle(player.getX(), player.getY(), 12);
        shapes.circle(ai.getX(), ai.getY(), 12);
        shapes.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}

