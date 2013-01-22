# handlebars-clj

This simple [Clojure](http://clojure.org) library extends
[Hiccup](https://github.com/weavejester/hiccup) expressions to support
[Handlebars.js](http://www.handlebarsjs.com/) style templating.  
 
The only reason to use HB templates in Clojure is if you want to
manipulate templates in both Clojure and Javascript environments
and want to edit them in a single place (e.g. Clojure server-side code),
otherwise you'd be better off using plain Hiccup.

HB templates can be resolved into plain hiccup expressions on the
server by applying a clojure map to the template function, or
converted into hiccup + handlebars templating annotations and into an
HTML template string for the client.

One design choice was to make the templating solution part of the
hiccup data structure tree, rather than a pure text solution so it's
easier to manipulate and construct templates out of component parts.
The downside is that clojure's syntax doesn't provide a direct analog
to the semantics of handlebar's templates.

Last update: January 21st, 2013

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
	  
You can resolve the template on the server,

    (post-view example-post)
    =>
    [:div.entry
      [:h1 "My first post"]
      [:h2 "By Charles Jolley"]]

inject handlebars compatible strings into Hiccup,

    (post-view)

    =>

    [:div.entry
     [:h1 "{{title}}"]
     "{{#with author}}"
     [:h2 "By " "{{firstName}}" "{{lastName}}"]
     "{{/with}}"

or render a complete handlebars template strings for the client.

    (html (post-view))

     => 

     <div class="entry">
       <h1>{{title}}</h1>

       {{#with author}}
       <h2>By {{firstName}} {{lastName}}</h2>
       {{/with}}
     </div>

A convenience function is provided to inject a template into your 
existing hiccup-based page generation logic.

    (inline-template name post-view)
    
    => [:script {:type "text/x-jquery-html" :id <name>}
         [:div.entry ...]]

    
## API and Use Cases

### Variable substitution

    (% variable)                 ;; {{variable}}

This HB template expression represents standard variable substitution
from a context map directly into the expression tree.  When the
template is run, it extracts the field directly from the context using
the keyword version of the variable and replace the expression with
it's value.  The resulting value can be any hiccup-compatible
expression.

    (% variable.child1.child2)   ;; {{variable.child1.child2}}

    ;; +
    ;; {:test "Empty" :variable {:child1 {:child2 "Nested value"} :name "One deep} 
    ;; => "Nested Value"

Path based variable substitution.  

    (% ../variable)              ;; {{../variable}}

If you are in a nested context, you can get to the outermost data
structure by using the parent path designation "../"

### Special Forms

#### str

Special form to allow you to inject multiple templates inline but
retain whitespace separation between the substituted forms when
rendered to HTML.

    (%str & forms)

#### strcat

Inserts the forms with intervening whitespace (e.g. join(" ")) as
appropriate.

    (%strcat & forms)
    
Concatenates the forms to remove whitespace, as appropriate

### Built-in Block Forms

Block forms in handlebars consist of a begin and end tag and take a
single variable as argument.  

    {{#with author}}
    <div id="{{../type}}-author">
      <h2>By {{firstName}} {{lastName}}</h2>
    </div>
    {{/with}}
      
The tag name designates a 'helper
function' which uses the variable value to determine how to process
the body of the template, typically by manipulating the current
context.

#### with

    (%with author                
      [:div {:id (%strcat {{../type}} "-author")}
        (%str "By " (% firstName) (% lastName))])
      
Block forms change the context of any template references inside their
body by making the new context a nested structure.  The parent
reference allows you to get to the original outer form from an
internal template.

#### if

    (%if var & forms)
      
Conditional form.  If slot is a non-null value, evaluate the body but
not in any specific context

#### unless

    (%unless var & forms)

Inverse conditional form.    

#### each

    (%each var & forms)
    
The iteration form combines repetition with context change.  The body
forms are duplicated once in the context of each element of the value
referred to by var.

### Define and use HB templates

    (deftemplate template-name
      <hiccup expression>)
	=> fn
      
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

## Notes and ToDo

This was a quick and dirty version, so there are a few small issues:

- No support for an {{else}} clause for %if forms
- No support for triple-slash unescaped string injection
- No integration or support for compile-time optimizations

It should be reasonably easy to extend this model to support other
templating models with similar semantics but different syntax.  I'm
sure there are opportunities to simplify both implementation and
interface; file tickets or pull requests on github as you see fit.  

