==================================================================
Download Continuuity Reactor SDK, |br| Install and Run Quick Start
==================================================================

.. reST Editor: .. section-numbering::
.. reST Editor: .. contents::

.. rst2pdf: CutStart
.. Slide Presentation HTML Generation
.. landslide: theme ../_theme/slides-generation/
.. landslide: build ../../html/

.. include:: ../_slide-fragments/continuuity_logo_copyright.rst

.. |br| raw:: html

   <br />
.. rst2pdf: CutStop

.. rst2pdf: config ../../../developer-guide/source/_templates/pdf-config
.. rst2pdf: stylesheets ../../../developer-guide/source/_templates/pdf-stylesheet
.. rst2pdf: build ../../pdf/
.. rst2pdf: .. |br|  unicode:: U+0020 .. space

----

Exercise Objectives
====================

In this exercise, you will:

- Download and install the SDK
- Learn basic Continuuity Reactor operations: Start and Stop
- Run the Quick Start tour

----

Exercise Steps
========================

.. - Go to Continuuity Reactor Download: ``http://www.continuuity.com/download``
.. - Register for free account
.. - Download ``continuuity-sdk-<version>.zip``

- Download ``continuuity-sdk-2.2.2.zip`` from |br|
  `https://repository.continuuity.com/content/groups/ 
  releases/com/continuuity/continuuity-sdk/2.2.2/ <https://repository.continuuity.com/content/groups/releases/com/continuuity/continuuity-sdk/2.2.2/>`__
- Unzip the download
- Open a command line window, go to ``/bin`` and run ``reactor.sh start``
- Open a browser window, go to ``http://localhost:9999``
- Follow the steps of the *Log Analytics Application* Quick Start Tour
- When finished, shut down the Reactor with ``reactor.sh stop``

.. These steps are online at ``http://continuuity.com/developers/quickstart``

These steps are online at `<http://continuuity.com/docs/reactor/2.2.2/en/quickstart.html>`__

----

Exercise Summary
===================

You should now:

- Be able to perform basic Reactor operations of Start and Stop
- Have taken the Quick Start tour
- Be able to use the Dashboard for basic operations
- Be able to inject and query data through the Dashboard
- Be able to modify an Application
- Be able to redeploy and restart an Application

----

Exercise Completed
==================

`Chapter Index <return.html#e01>`__

