package io.awallet.crypto.alphawallet;

import java.util.HashMap;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by weiwu on 7/4/18.
 */

public class RxExperiment {

     class Animal {
        protected HashMap<String, String> attributes = new HashMap();
        protected String colour;

        public String getAttribute(String attributeName) {
            return attributes.get(attributeName);
        }
        /* We ignore the fact that attributes affects tokenID for now */
        public void setAttribute(String attributeName, String attributeValue) {
            attributes.put(attributeName, attributeValue);
        }
    }

    @Test
    public void FilterMapReduceShouldAllWork() {
        Single<Long> count;
        Animal kitty = new Animal();
        kitty.setAttribute("name", "Kitty");
        kitty.setAttribute("colour", "ginger");
        Animal luna = new Animal();
        luna.setAttribute("name", "Luna");
        luna.setAttribute("colour", "white");
        Observable<Animal> animals = Observable.fromArray(kitty, luna);

        count = animals.filter(t -> t.getAttribute("name") == "Kitty").count();
        count.subscribe(c -> assertEquals(1L, (long)c));

        animals.map(animal -> animal.getAttribute("name"))
                .reduce((a, b) -> a + b)
                .subscribe(name -> assertEquals(name, "KittyLuna"));
    }

    @Test
    public void GroupByShouldWork() {
        Observable.range(1, 20)
                .groupBy(v -> v % 2 == 0)
                .flatMapMaybe(group -> {
                    return group.reduce((a, b) -> a + b)
                            .map(v -> "Group " + group.getKey() + " sum is " + v);
                })
                .subscribe(System.out::println);
    }
}
