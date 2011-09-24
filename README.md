# handlebars-clj

This simple [Clojure](http://clojure.org) library extends
[Hiccup](https://github.com/weavejester/hiccup) expressions to support
[handlebars](http://www.handlebarsjs.com/) style templating.  We'll
call these extended expressions HB templates.
 
The only good reason to use HB templates is if you want to define and
use templates in clojure code that you can use either on the server or
the client, otherwise you'd be much better off sticking with straight
Clojure for injecting clojure values into hiccup expressions!

HB templates can be resolved into plain hiccup expressions on the
server by applying a clojure map to the template, or converted into
hiccup + handlebars templating annotations and into an HTML template
string for the client.

One design choice was to make the templating solution part of the
hiccup data structure tree, rather than a pure text solution so it's
easier to manipulate and construct templates out of component parts.
The downside is that clojure's syntax doesn't provide a direct analog
to the semantics of handlebar's templates.

## Example

A blog post

    (def example-post
      {
       :title "My first post"
       :author {
          :firstName "Charles"
	  :lastName  "Jolley"
	}
      })
    
The hiccup+HB template

    (deftemplate post-view
      [:div.entry
        [:h1 (% title)]
	(%with author
	  [:h2 (%str "By " (% firstName) (% lastName))])])
	  
Resolve the template on the server

    (post-view example-post)
    =>
    [:div.entry
      [:h1 "My first post"]
      [:h2 "By Charles Jolley"]]

Or send to the client as a template

    (post-view)

    => translates to

    [:div.entry
     [:h1 "{{title}}"]
     "{{#with author}}"
     [:h2 "By " "{{firstName}}" "{{lastName}}"]
     "{{/with}}"

     => renders as    

     <div class="entry">
       <h1>{{title}}</h1>

       {{#with author}}
       <h2>By {{firstName}} {{lastName}}</h2>
       {{/with}}
     </div>
    
## API

### Variable substitution

    (% variable)

Standard variable substitution.  Extract the field directly from the
context using the keyword version of the variable and replace the
expression with it's value.  The resulting value can be any
hiccup-compatible expression.

    (% variable.child1.child2)

    ;; +
    ;; {:test "Empty" :variable {:child1 {:child2 "Nested value"} :name "One deep} 
    ;; => "Nested Value"

Path based variable substitution.  

    (% ../variable)

If you are in a nested context, you can get to the outermost data
structure by using the parent path designation "../"

### Built-in Block Forms

    (%with author
      [:div (%str "By " (% firstName) (% lastName))])
      
Block forms change the context of any template references inside their
body by making the new context a nested structure.  The parent
reference allows you to get to the original outer form from an
internal template.

    (%each var & forms)
    
The iteration form combines repetition with context change.  The body
forms are duplicated once in the context of each element of the value
referred to by var.

    (%if var & forms)
      
Conditional form.  If slot is a non-null value, evaluate the body but
not in any specific context

    (%unless var & forms)

Inverse conditional form.    

    (%str & forms) 

Special form to allow you to inject multiple templates inline but
retain whitespace separation between the substituted forms when
rendered to HTML.

### Define and use templates

    (deftemplate template-name
      <hiccup expression>)
      
    (template-name context) => hiccup expression
    
    (template-name) => hiccup + handlebars strings

Calling the form with a map returns a hiccup expression with
substitutions, otherwise it generates handlebars template strings.

### Create custom block forms

    (def ^:dynamic *context* nil)
    (resolve-var var)
    (defhelper name description argnames & body)

    e.g.

    (defhelper each
      "Description"
      [var template-fn]
      (mapcat fn (resolve-var var)))
      
You can create a new helper function as shown above. The macro
generates a helper function '%helper-<tag>' and a macro '%<tag>' to
simplify generating script bodies.  The function body accepts a
variable name to lookup in the current context (use resolve-var

## Notes

This was a quick and dirty version, so there are a few small oddities.

- No support for an {{else}} clause for %if forms
- No support for triple-slash unescaping
- No integration or support for compile-time optimizations

