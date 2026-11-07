package wealthsei.presentation.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.FixedBill;
import wealthsei.domain.Frequency;
import wealthsei.domain.IncomeSource;

/** The small forms behind "Edit plan" and "Set a budget limit". Each one reads the answers, calls the facade, and reports with a toast. */
public final class PlanDialogs {

    private PlanDialogs() {
    }

    /** What the menu next to "Edit plan" offers, in order. */
    public static List<Menus.Option> planMenu() {
        return List.of(new Menus.Option("Set my balance"), new Menus.Option("Add income"),
                new Menus.Option("Add a fixed bill"), new Menus.Option("Set safety buffer"));
    }

    public static void runPlanChoice(AppContext ctx, int choice) {
        switch (choice) {
            case 0 -> balance(ctx);
            case 1 -> income(ctx);
            case 2 -> bill(ctx);
            default -> buffer(ctx);
        }
    }

    private static Frequency frequency(String text) {
        try {
            return Frequency.valueOf(text.trim().toUpperCase(Locale.ENGLISH));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Repeats must be weekly, biweekly, monthly or yearly");
        }
    }

    public static void balance(AppContext ctx) {
        Optional<FinancialPlan> plan = ctx.facade().getPlan();
        String[] v = Dialogs.form(ctx.window(), "Your balance", "What is in your account today? Forecasts and safe-to-spend start from this.",
                new String[] {"Balance today"}, new String[] {plan.map(p -> p.currentBalance().amount().toPlainString()).orElse("")}, "Save");
        if (v == null) {
            return;
        }
        run(ctx, "Balance saved.", () -> ctx.facade().setBalance(Dialogs.money(v[0]), ctx.today()));
    }

    public static void income(AppContext ctx) {
        String[] v = Dialogs.form(ctx.window(), "Add income", "A paycheque or any money that arrives on a schedule.",
                new String[] {"Name", "Amount each time", "Repeats (weekly, biweekly, monthly, yearly)", "Next payday (YYYY-MM-DD)"},
                new String[] {"Paycheque", "", "biweekly", ctx.today().plusDays(7).toString()}, "Add");
        if (v == null) {
            return;
        }
        run(ctx, "Income added.", () -> ctx.facade().addIncome(new IncomeSource(required(v[0], "name"), Dialogs.money(v[1]),
                frequency(v[2]), Dialogs.date(v[3]))));
    }

    public static void bill(AppContext ctx) {
        String[] v = Dialogs.form(ctx.window(), "Add a fixed bill", "Rent, phone, insurance: anything that leaves your account on a schedule.",
                new String[] {"Name", "Amount each time", "Repeats (weekly, biweekly, monthly, yearly)", "Next due date (YYYY-MM-DD)", "Category"},
                new String[] {"Rent", "", "monthly", ctx.today().plusDays(10).toString(), "Rent"}, "Add");
        if (v == null) {
            return;
        }
        run(ctx, "Bill added.", () -> {
            Category c = Categories.defaults().stream().filter(x -> x.name().equalsIgnoreCase(v[4]) || x.id().equalsIgnoreCase(v[4]))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown category '" + v[4] + "'. Try Rent, Utilities, Subscriptions..."));
            return ctx.facade().addFixedBill(new FixedBill(required(v[0], "name"), Dialogs.money(v[1]), frequency(v[2]), Dialogs.date(v[3]), c));
        });
    }

    public static void buffer(AppContext ctx) {
        Optional<FinancialPlan> plan = ctx.facade().getPlan();
        String[] v = Dialogs.form(ctx.window(), "Safety buffer", "Money you want to always keep untouched. It is left out of safe-to-spend.",
                new String[] {"Buffer"}, new String[] {plan.map(p -> p.safetyBuffer().amount().toPlainString()).orElse("200")}, "Save");
        if (v == null) {
            return;
        }
        run(ctx, "Safety buffer saved.", () -> ctx.facade().setSafetyBuffer(Dialogs.money(v[0])));
    }

    /** Picks a category from a menu under {@code anchor}, asks for its monthly limit, and saves it. */
    public static void budgetLimit(AppContext ctx, java.awt.Component anchor, java.time.YearMonth month) {
        List<Category> cats = new ArrayList<>();
        List<Menus.Option> options = new ArrayList<>();
        for (Category c : Categories.defaults()) {
            if (!c.equals(Categories.INCOME)) {
                cats.add(c);
                options.add(new Menus.Option(c.name(), Theme.categoryColour(c)));
            }
        }
        Menus.popup(anchor, options, -1, i -> {
            Category c = cats.get(i);
            String[] v = Dialogs.form(ctx.window(), "Limit for " + c.name(), "The most you want to spend on " + c.name().toLowerCase(Locale.ENGLISH)
                    + " in " + month.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + ". You get an alert as you get close.",
                    new String[] {"Monthly limit"}, new String[] {""}, "Save limit");
            if (v == null) {
                return;
            }
            run(ctx, c.name() + " limit saved.", () -> ctx.facade().setBudgetLimit(month, c, Dialogs.money(v[0])));
        });
    }

    private static String required(String s, String what) {
        if (s.isBlank()) {
            throw new IllegalArgumentException("Please enter a " + what);
        }
        return s;
    }

    private static void run(AppContext ctx, String done, java.util.function.Supplier<Object> action) {
        try {
            action.get();
            ctx.changed();
            ctx.toast(done, Toast.Kind.SUCCESS);
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage() == null ? "That did not work." : e.getMessage(), Toast.Kind.ERROR);
        }
    }
}
